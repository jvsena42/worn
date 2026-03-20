# App Architecture

> **Stack:** Kotlin Multiplatform (Android + iOS) · MVI + Repository Pattern · Claude AI

---

## 1. Overview

This document describes the architecture of **a KMP wardrobe manager app** for Android and iOS. Users can catalog their clothing items with photos stored in app-private internal storage. AI features (image analysis and auto-tagging) are powered by the Anthropic Claude API, using an API key provided by the user and stored as an encrypted secret.

---

## 2. Pattern: MVI + Repository

No Use Cases layer. Business logic lives directly in the Repository implementations, keeping the architecture simple without sacrificing testability or separation of concerns.

### Data flow

```
User Action
    │
    ▼
Intent (sealed class)
    │
    ▼
ViewModel  ──► Repository Interface
    │                  │
    ▼         Repository Implementation
  State         (business logic here)
    │                  │
    ▼            ┌─────┴──────┐
UI (Compose /   LocalSource  AiSource
  SwiftUI)    (photos + db)  (Claude API)
```

---

## 3. Module Structure

```
root/
├── androidApp/                  # Android entry point
├── iosApp/                      # iOS entry point (Xcode project)
└── shared/                      # KMP shared module
    ├── commonMain/
    │   ├── data/
    │   │   ├── repository/      # Repository implementations (business logic)
    │   │   └── source/
    │   │       ├── local/       # SQLDelight DB + file storage abstraction
    │   │       └── remote/      # Claude API client
    │   ├── domain/
    │   │   ├── model/           # Pure Kotlin data classes
    │   │   └── repository/      # Repository interfaces
    │   ├── presentation/
    │   │   └── viewmodel/       # Shared ViewModels (MVI)
    │   └── util/
    │       └── secret/          # SecretStore interface
    ├── androidMain/
    │   ├── source/local/        # Android file I/O
    │   └── secret/              # EncryptedSharedPreferences
    ├── iosMain/
    │   ├── source/local/        # iOS file I/O
    │   └── secret/              # Keychain
    └── commonTest/
        └── repository/          # Unit tests (all repositories)
```

---

## 4. Layer Responsibilities

### 4.1 Domain Layer (`shared/commonMain/domain/`)

Pure Kotlin — no platform imports, no framework dependencies.

```kotlin
// Models
data class ClothingItem(
    val id: String,
    val name: String,
    val category: Category,
    val colors: List<String>,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val photoPath: String,
    val createdAt: Long
)

enum class Category {
    TOP, BOTTOM, DRESS, OUTERWEAR, SHOES, ACCESSORY
}

data class AiAnalysisResult(
    val description: String,
    val suggestedCategory: Category,
    val colors: List<String>,
    val tags: List<String>
)

// Repository interfaces
interface WardrobeRepository {
    suspend fun getAll(): List<ClothingItem>
    suspend fun getById(id: String): ClothingItem?
    suspend fun getByCategory(category: Category): List<ClothingItem>
    suspend fun search(query: String): List<ClothingItem>
    suspend fun addItem(imageBytes: ByteArray, name: String): ClothingItem
    suspend fun analyzeAndTag(itemId: String): ClothingItem
    suspend fun updateItem(item: ClothingItem): ClothingItem
    suspend fun deleteItem(id: String)
}

interface SecretStore {
    fun getApiKey(): String?
    fun saveApiKey(key: String)
    fun clearApiKey()
}
```

### 4.2 Data Layer (`shared/commonMain/data/`)

Repository implementations contain all business logic — validation, orchestration between local and AI sources, error handling.

```kotlin
class WardrobeRepositoryImpl(
    private val db: WardrobeDatabase,          // SQLDelight
    private val fileStorage: PhotoFileStorage,  // expect/actual
    private val aiClient: ClaudeApiClient
) : WardrobeRepository {

    // Business logic: save photo, persist metadata, return item
    override suspend fun addItem(imageBytes: ByteArray, name: String): ClothingItem {
        val path = fileStorage.write("${uuid()}.jpg", imageBytes)
        val item = ClothingItem(
            id = uuid(),
            name = name,
            category = Category.TOP, // default until analyzed
            colors = emptyList(),
            photoPath = path,
            createdAt = currentTimeMillis()
        )
        db.clothingItemQueries.insert(item)
        return item
    }

    // Business logic: call AI, map result, persist updated item
    override suspend fun analyzeAndTag(itemId: String): ClothingItem {
        val item = getById(itemId) ?: error("Item not found: $itemId")
        val imageBytes = fileStorage.read(item.photoPath)
        val analysis = aiClient.analyzeImage(imageBytes)
        val updated = item.copy(
            description = analysis.description,
            category = analysis.suggestedCategory,
            colors = analysis.colors,
            tags = analysis.tags
        )
        db.clothingItemQueries.update(updated)
        return updated
    }

    // Business logic: also deletes photo file
    override suspend fun deleteItem(id: String) {
        val item = getById(id) ?: return
        fileStorage.delete(item.photoPath)
        db.clothingItemQueries.delete(id)
    }

    override suspend fun search(query: String): List<ClothingItem> =
        db.clothingItemQueries.search("%$query%").executeAsList().map { it.toDomain() }
}
```

**Local photo storage** — app-private only, via `expect/actual`:

```kotlin
expect class PhotoFileStorage {
    suspend fun write(fileName: String, bytes: ByteArray): String
    suspend fun read(filePath: String): ByteArray
    suspend fun delete(filePath: String)
}
```

**Claude API client** — thin HTTP wrapper, no business logic:

```kotlin
class ClaudeApiClient(private val secretStore: SecretStore) {
    suspend fun analyzeImage(imageBytes: ByteArray): AiAnalysisResult
    // Sends image to claude-sonnet-4-20250514 via /v1/messages
    // Parses response into AiAnalysisResult
}
```

### 4.3 Presentation Layer (`shared/commonMain/presentation/`)

ViewModels are thin — they call the repository and map results to UI state. No business logic here.

```kotlin
sealed class WardrobeIntent {
    object LoadItems : WardrobeIntent()
    data class AddItem(val imageBytes: ByteArray, val name: String) : WardrobeIntent()
    data class AnalyzeItem(val itemId: String) : WardrobeIntent()
    data class DeleteItem(val itemId: String) : WardrobeIntent()
    data class Search(val query: String) : WardrobeIntent()
    data class FilterByCategory(val category: Category?) : WardrobeIntent()
}

data class WardrobeState(
    val items: List<ClothingItem> = emptyList(),
    val isLoading: Boolean = false,
    val analyzingItemId: String? = null,
    val activeCategory: Category? = null,
    val searchQuery: String = "",
    val error: String? = null
)

sealed class WardrobeEffect {
    data class ShowError(val message: String) : WardrobeEffect()
    object ItemAdded : WardrobeEffect()
    object ItemDeleted : WardrobeEffect()
}

class WardrobeViewModel(
    private val repository: WardrobeRepository
) : ViewModel() {
    val state: StateFlow<WardrobeState>
    val effects: Flow<WardrobeEffect>
    fun onIntent(intent: WardrobeIntent)
}
```

---

## 5. AI Integration (Claude API)

- The API key is **user-provided** and stored in the platform secret store — never hardcoded.
- Photos are sent as base64-encoded images to `claude-sonnet-4-20250514`.
- The client is a thin wrapper; all decisions about *when* and *how* to call AI live in the Repository.

---

## 6. Security

| Concern | Solution |
|---|---|
| API key at rest | Android: `EncryptedSharedPreferences` · iOS: Keychain |
| API key in transit | HTTPS only (Ktor) |
| Photo data | App-private internal storage — no external app access |
| No key in logs | `SecretStore` never exposes raw key to logging layers |
| No key in source | User-provided at runtime |

---

## 7. Testing Strategy

Repositories are the main unit test target — they hold the business logic.

```
shared/commonTest/
├── repository/
│   └── WardrobeRepositoryTest.kt
└── viewmodel/
    └── WardrobeViewModelTest.kt
```

```kotlin
class WardrobeRepositoryTest {
    private val fakeDb = FakeWardrobeDatabase()
    private val fakeStorage = FakePhotoFileStorage()
    private val fakeAiClient = FakeClaudeApiClient()
    private val repository = WardrobeRepositoryImpl(fakeDb, fakeStorage, fakeAiClient)

    @Test
    fun `adding item saves photo and persists metadata`() = runTest {
        val item = repository.addItem(imageBytes = byteArrayOf(1, 2, 3), name = "Blue Jeans")
        assertEquals("Blue Jeans", item.name)
        assertTrue(fakeStorage.hasFile(item.photoPath))
        assertNotNull(fakeDb.findById(item.id))
    }

    @Test
    fun `analyzeAndTag updates category colors and tags`() = runTest {
        val item = repository.addItem(byteArrayOf(), "Jacket")
        fakeAiClient.willReturn(AiAnalysisResult(
            description = "A navy blue jacket",
            suggestedCategory = Category.OUTERWEAR,
            colors = listOf("navy"),
            tags = listOf("jacket", "formal")
        ))

        val updated = repository.analyzeAndTag(item.id)

        assertEquals(Category.OUTERWEAR, updated.category)
        assertEquals(listOf("navy"), updated.colors)
        assertEquals(listOf("jacket", "formal"), updated.tags)
    }

    @Test
    fun `deleting item removes photo file and db record`() = runTest {
        val item = repository.addItem(byteArrayOf(), "Old Shirt")
        repository.deleteItem(item.id)
        assertFalse(fakeStorage.hasFile(item.photoPath))
        assertNull(fakeDb.findById(item.id))
    }
}
```

---

## 8. Key Dependencies

| Library | Purpose |
|---|---|
| `kotlinx-coroutines` | Async / Flow |
| `ktor-client` | HTTP client for Claude API (multiplatform) |
| `kotlinx-serialization` | JSON parsing |
| `SQLDelight` | Local clothing metadata DB (multiplatform) |
| `Koin` | Dependency injection (multiplatform) |
| `androidx.security` | EncryptedSharedPreferences (Android only) |
| `kotlin-test` | Unit testing in commonTest |

---

## 9. Decision Log

| Decision | Rationale |
|---|---|
| No Use Cases layer | Avoids indirection for a focused single-domain app |
| Business logic in Repository | Orchestration of AI + storage + DB in one testable place |
| MVI for presentation | Unidirectional flow handles AI async states cleanly |
| App-private storage | No permission requests; simpler security model |
| Koin over Hilt | Multiplatform; Hilt is Android-only |
| SQLDelight | Only multiplatform SQL solution with type-safe queries |
