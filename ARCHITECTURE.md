# App Architecture

> **Stack:** Kotlin Multiplatform (Android + iOS) · MVI + Repository Pattern · Claude API · on-device AI · YouCam Apparel VTO

---

## 1. Overview

This document describes the architecture of **Worn**, a KMP wardrobe manager for Android and iOS. Users catalog clothing items with photos stored in app-private internal storage, assemble outfits from them, and see which items are missing from their wardrobe.

Three AI-backed capabilities sit on top of that catalog, each independently opt-in and each using a credential the user supplies (BYOK — nothing is bundled in the build):

- **Photo analysis and auto-tagging**, plus gap recommendations and prospective-purchase analysis — served by either the Anthropic **Claude API** or the device's **on-device model**, behind one interface.
- **Virtual try-on** — Perfect Corp's **YouCam Apparel VTO** renders a garment onto the user's saved model photo.
- **Background removal** — ML Kit Subject Segmentation, on-device, no credential required.

---

## 2. Pattern: MVI + Repository

No Use Cases layer. Business logic lives directly in the Repository implementations, keeping the architecture simple without sacrificing testability or separation of concerns.

### Data flow

```
User Action
    │
    ▼
Intent (sealed interface)
    │
    ▼
ViewModel  ──► Repository Interface
    │                  │
    ▼         Repository Implementation
  State         (business logic here)
    │                  │
    ▼        ┌─────────┼──────────┐
UI (Compose /│         │          │
  SwiftUI)   │         │          │
        LocalSource  AiSource  TryOnSource
       (photos + db) (Claude |  (YouCam)
                     on-device)
```

---

## 3. Module Structure

```
root/
├── composeApp/                      # Android application module (Compose UI)
├── iosApp/                          # iOS app (Xcode/SwiftUI) + WornShareExtension
├── journeys/                        # XML end-to-end journey specs
└── shared/                          # KMP shared module
    ├── commonMain/
    │   ├── data/
    │   │   ├── repository/          # Repository implementations (business logic)
    │   │   └── source/
    │   │       ├── local/           # SQLDelight DB, DataStore, PhotoFileStorage
    │   │       ├── remote/          # Claude + YouCam API clients
    │   │       ├── ai/              # Shared prompts, response models, parser, on-device engine
    │   │       └── image/           # BackgroundRemover (expect)
    │   ├── domain/
    │   │   ├── model/               # Pure Kotlin data classes
    │   │   └── repository/          # Repository interfaces
    │   ├── presentation/
    │   │   └── viewmodel/           # Shared ViewModels (MVI)
    │   ├── util/
    │   │   ├── secret/              # SecretStore interface
    │   │   ├── image/               # CropGeometry
    │   │   └── crypto/              # RsaEncryptor (expect)
    │   ├── di/                      # Koin modules
    │   └── sqldelight/              # .sq schemas
    ├── androidMain/                 # Keystore, AndroidSqliteDriver, ML Kit, OkHttp, JCA
    ├── iosMain/                     # Keychain, NativeSqliteDriver, Darwin, Security framework
    ├── commonTest/                  # ViewModel tests + fakes
    └── androidHostTest/             # Repository and API client tests (JVM)
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
    val seasons: List<Season>,
    val subcategory: Subcategory? = null,
    val fit: Fit? = null,
    val material: Material? = null,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val photoPath: String,
    val createdAt: Long
)

enum class Category { TOP, BOTTOM, DRESS, OUTERWEAR, SHOES, ACCESSORY }

/** Which YouCam endpoint family and garment_category a try-on uses. */
enum class GarmentCategory { TOP, BOTTOM, FULL_BODY, SHOES }

data class TryItResult(
    val matchingItems: List<ClothingItem>,
    val combinationsUnlocked: Int,
    val gapsFilled: List<String>,
    val worthAdding: Boolean,
)
```

Repository interfaces expose `kotlin.Result<T>` for one-shot operations, and plain `Flow<T>` for reactive reads (failures surface as stream exceptions, handled with `catch` by the collector).

```kotlin
interface WardrobeRepository {
    fun observeAll(): Flow<List<ClothingItem>>          // single source of truth for the UI
    suspend fun getAll(): Result<List<ClothingItem>>
    suspend fun search(query: String): Result<List<ClothingItem>>
    suspend fun addItem(imageBytes: ByteArray, name: String, /* ... */): Result<ClothingItem>
    suspend fun analyzeAndTag(itemId: String): Result<ClothingItem>
    suspend fun deleteItem(id: String): Result<Unit>
    suspend fun getGapRecommendations(): Result<List<GapRecommendation>>
    suspend fun analyzeProspectiveItem(imageBytes: ByteArray): Result<TryItResult>
}

interface TryOnRepository {
    suspend fun generateTryOn(garmentBytes: ByteArray, category: GarmentCategory): Result<ByteArray>
    suspend fun verifyCredentials(clientId: String, clientSecret: String): Result<Unit>
}

// Also: OutfitRepository, SettingsRepository (profile, model photo, credential state)
```

`SecretStore` is keyed by name so multiple providers coexist:

```kotlin
interface SecretStore {
    fun getSecret(name: String): String?
    fun saveSecret(name: String, value: String)
    fun clearSecret(name: String)

    companion object {
        const val CLAUDE_KEY = "claude_api_key"
        const val YOUCAM_CLIENT_ID = "youcam_client_id"
        const val YOUCAM_CLIENT_SECRET = "youcam_client_secret"
    }
}
```

### 4.2 Data Layer (`shared/commonMain/data/`)

Repository implementations contain all business logic — validation, orchestration between local, AI, and try-on sources, error handling. Two conventions apply throughout:

- **The `CoroutineContext` is injected via the constructor**, never hardcoded to `Dispatchers.IO`. Every DB, file, and network call is wrapped in `withContext(dispatcher)`; callers never switch dispatchers, and platform data sources don't dispatch on their own behalf.
- **`runCatching`, not `try/catch`**, so implementations return the `Result<T>` the interface promises.

```kotlin
class WardrobeRepositoryImpl(
    private val db: WardrobeDatabase,           // SQLDelight
    private val fileStorage: PhotoFileStorage,  // expect/actual
    private val aiClient: ClaudeApiClient,
    private val onDeviceAi: OnDeviceAiSource,
    private val settingsRepository: SettingsRepository,
    private val dispatcher: CoroutineContext,   // injected — never hardcoded
) : WardrobeRepository {

    // Business logic: pick a provider, call it, map the result, persist the updated item
    override suspend fun analyzeAndTag(itemId: String): Result<ClothingItem> = runCatching {
        withContext(dispatcher) {
            val item = findById(itemId) ?: error("Item not found: $itemId")
            val imageBytes = fileStorage.read(item.photoPath)
            val analysis = if (useOnDeviceAi()) {
                onDeviceAi.analyzeImage(imageBytes)
            } else {
                aiClient.analyzeImage(imageBytes)
            }
            db.clothingItemQueries.update(/* mapped fields */)
            item.copy(/* ... */)
        }
    }

    // Business logic: deleting an item cascades to its outfits and its photo file
    override suspend fun deleteItem(id: String): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val item = findById(id) ?: return@withContext
            db.transaction { /* delete affected outfits, then the item */ }
            fileStorage.delete(item.photoPath)
        }
    }
}
```

Note the nesting: `runCatching` is the outer wrapper and `withContext` the inner one, so a dispatcher failure is captured in the `Result` alongside everything else.

**Platform abstractions** — all `expect/actual`, with the platform-specific work kept behind a common signature:

```kotlin
expect class PhotoFileStorage {          // app-private internal storage
    suspend fun write(fileName: String, bytes: ByteArray): String
    suspend fun read(filePath: String): ByteArray
    suspend fun delete(filePath: String)
}

expect class BackgroundRemover {         // ML Kit on Android, Vision on iOS
    suspend fun removeBackground(bytes: ByteArray): ByteArray
}

expect class RsaEncryptor {              // JCA on Android, Security framework on iOS
    fun encrypt(plaintext: String, publicKeyBase64: String): String
}
```

**API clients** are thin HTTP wrappers with no business logic — `ClaudeApiClient` and `YouCamApiClient`. All decisions about *when* and *how* to call them live in the repositories.

### 4.3 Presentation Layer (`shared/commonMain/presentation/`)

ViewModels are thin — they call the repository, consume the `Result` directly with `.onSuccess`/`.onFailure` (never `try/catch` or `runCatching` of their own), and map to UI state. No business logic here.

```kotlin
sealed interface TryItIntent {
    data class GarmentSelected(val imageBytes: ByteArray) : TryItIntent
    data class CategorySelected(val category: GarmentCategory) : TryItIntent
    data object GenerateTryOn : TryItIntent
    data object AnalyzeItem : TryItIntent
}

data class TryItState(
    val hasClaudeKey: Boolean = false,
    val hasYouCamKey: Boolean = false,
    val personImage: ByteArray? = null,
    val selectedCategory: GarmentCategory? = null,
    val tryOnLoading: Boolean = false,
    val tryOnImage: ByteArray? = null,
    val tryOnError: String? = null,
    val analysis: TryItResult? = null,
)

class TryItViewModel(
    private val tryOnRepository: TryOnRepository,
    private val wardrobeRepository: WardrobeRepository,
) : ViewModel() {
    val state: StateFlow<TryItState>
    val effects: Flow<TryItEffect>
    fun onIntent(intent: TryItIntent)
}
```

The Android UI is Compose Multiplatform; the iOS UI is native SwiftUI driven by wrapper classes that bridge the shared ViewModels. Both consume the same state.

---

## 5. AI & Try-On Integration

### 5.1 Claude API

- The API key is **user-provided** and stored in the platform secret store — never hardcoded.
- Photos are sent as base64-encoded images to the Messages API.
- The client is a thin wrapper; all decisions about *when* and *how* to call AI live in the repository.

### 5.2 On-device AI

An alternative provider the repository selects per call, based on the user's setting: **ML Kit GenAI Prompt** on Android, **Apple Intelligence** (`FoundationModels`, reached through a small Swift bridge) on iOS.

Prompts, response models, and parsing are shared across both providers in `data/source/ai/` (`AiPrompts`, `AiResponseModels`, `AiResponseParser`), so switching providers doesn't fork the prompt logic. Availability is a first-class domain model (`OnDeviceAiAvailability`, with an `OnDeviceAiUnavailableReason`) because support varies by device, OS version, and user opt-in.

One deliberate exception: `analyzeProspectiveItem` — the Try It analysis — stays on Claude regardless of the preference, because reasoning over the whole wardrobe against a new photo is a task small on-device models handle poorly.

### 5.3 YouCam Apparel VTO

Server-to-server against `https://yce-api-01.perfectcorp.com`, from shared Kotlin, with no SDK and no backend of our own:

**authenticate → presigned upload (person + garment) → create task → poll → download JPEG**

- **Auth is RSA, not a bearer secret.** The `id_token` is `client_id=<id>&timestamp=<ms>` encrypted with the user's public key under RSA/PKCS#1 v1.5, Base64'd. Access tokens are cached for their 2h TTL with a 5-minute refresh margin.
- The `expect/actual` `RsaEncryptor` exists because the platforms disagree on key format: Android's `X509EncodedKeySpec` takes the X.509 SPKI key the portal issues, while iOS's `SecKeyCreateWithData` requires PKCS#1 — so the iOS actual walks the ASN.1 TLV structure to unwrap the inner `RSAPublicKey`.
- **Endpoint family is routed by garment category**: `v2.0`/`cloth-v3` with `garment_category` = `upper_body`/`lower_body`/`full_body`, or `v1.0`/`shoes` (no garment category).
- Polling runs every 2s for up to 60 attempts; HTTP status codes map to user-actionable messages (401/403 credentials, 429 quota, 5xx service).

See the README for the full walkthrough.

---

## 6. Security

| Concern | Solution |
|---|---|
| Secrets at rest | Android: Keystore AES/GCM-encrypted per-name file · iOS: Keychain |
| Secrets in transit | HTTPS only (Ktor) |
| Photo data | App-private internal storage — no external app access, no storage permission |
| No secret in logs | Only lengths are logged; `SecretStore` never exposes raw values to logging layers |
| No secret in source | User-provided at runtime (BYOK); nothing in the build, gradle, or CI |
| Bad credentials | Verified with a live auth handshake **before** being persisted |
| Key-value storage | DataStore for non-secret preferences; the Keystore path is used only where encryption is required |

---

## 7. Testing Strategy

Repositories hold the business logic, so they are the main unit test target. ViewModel state transitions are asserted with Turbine.

```
shared/
├── commonTest/                     # multiplatform
│   ├── viewmodel/                  # Wardrobe, Outfit, Settings, TryIt
│   ├── ai/                         # OnDeviceAiSourceTest
│   ├── util/image/                 # CropGeometryTest
│   ├── model/                      # AppShortcutTest
│   └── fake/                       # fakes for every repository + SecretStore
└── androidHostTest/                # JVM-only (needs JCA, MockK)
    ├── repository/                 # WardrobeRepositoryImpl, TryOnRepositoryImpl
    ├── remote/                     # ClaudeApiClientTest, YouCamApiClientTest
    └── util/crypto/                # RsaEncryptorTest
```

- **`ktor-client-mock`** drives the API client tests, so the full YouCam auth → upload → poll → download sequence is exercised without network access.
- **Turbine** asserts `StateFlow` / effect emissions; **MockK** covers the JVM-side doubles.
- **`journeys/`** holds XML end-to-end journey specs run by the `android` CLI against a real device (see [`journeys/README.md`](journeys/README.md)). Compose test tags are mirrored as iOS `accessibilityIdentifier` values so one spec describes both platforms.

```kotlin
@Test
fun `generateTryOn fails when no model photo is saved`() = runTest {
    val repository = TryOnRepositoryImpl(youCamClient, fakeSettings, testDispatcher)
    val result = repository.generateTryOn(garmentBytes = byteArrayOf(1, 2, 3), category = TOP)
    assertTrue(result.isFailure)
}
```

---

## 8. Key Dependencies

| Library | Purpose |
|---|---|
| `kotlinx-coroutines` | Async / Flow |
| `ktor-client` | HTTP client for Claude + YouCam (multiplatform) |
| `kotlinx-serialization` | JSON parsing |
| `SQLDelight` | Local clothing metadata DB (multiplatform) |
| `Koin` | Dependency injection (multiplatform) |
| `Coil` | Image loading |
| `androidx-datastore` | Key-value storage for non-secret preferences |
| `mlkit-genai-prompt` | On-device AI (Android) |
| `play-services-mlkit-subject-segmentation` | Background removal (Android) |
| `kotlin-test` · `turbine` · `mockk` · `ktor-client-mock` | Testing |
| `detekt` | Static analysis |

---

## 9. Decision Log

| Decision | Rationale |
|---|---|
| No Use Cases layer | Avoids indirection for a focused single-domain app |
| Business logic in Repository | Orchestration of AI + try-on + storage + DB in one testable place |
| MVI for presentation | Unidirectional flow handles AI async states cleanly |
| App-private storage | No permission requests; simpler security model |
| Koin over Hilt | Multiplatform; Hilt is Android-only |
| SQLDelight | Only multiplatform SQL solution with type-safe queries |
| BYOK over bundled keys | No credential ships in the binary; each AI feature is independently opt-in and the app is useful with none of them |
| `CoroutineContext` injected via constructor | Dependency inversion — repositories are testable with a test dispatcher and never hardcode `Dispatchers.IO` |
| Repositories return `Result<T>` | Errors are values at the layer boundary; ViewModels consume them without `try/catch` |
| DataStore over SharedPreferences | Async, type-safe, transactional; the Keystore path is reserved for secrets that need real encryption |
| On-device AI as a swappable provider | Shared prompts and parsing, so the repository picks a provider per call and analysis can run offline and free |
| `expect/actual` RSA | No multiplatform library covers PKCS#1 v1.5 on both targets, and the two platforms disagree on public-key encoding (SPKI vs PKCS#1) |
| Native SwiftUI on iOS | Compose Multiplatform for Android UI, native SwiftUI on iOS, sharing only the ViewModels — platform-idiomatic UI on both |
