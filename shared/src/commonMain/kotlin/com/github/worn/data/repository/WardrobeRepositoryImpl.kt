package com.github.worn.data.repository

import com.github.worn.data.source.local.PhotoFileStorage
import com.github.worn.data.source.local.db.WardrobeDatabase
import com.github.worn.data.source.remote.ClaudeApiClient
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.TryItResult
import com.github.worn.domain.repository.WardrobeRepository
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import com.github.worn.data.source.local.db.ClothingItem as DbClothingItem

@OptIn(ExperimentalUuidApi::class)
class WardrobeRepositoryImpl(
    private val db: WardrobeDatabase,
    private val fileStorage: PhotoFileStorage,
    private val aiClient: ClaudeApiClient,
    private val dispatcher: CoroutineContext,
) : WardrobeRepository {

    override suspend fun getAll(): Result<List<ClothingItem>> = runCatching {
        withContext(dispatcher) {
            db.clothingItemQueries.getAll().executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun getById(id: String): Result<ClothingItem?> = runCatching {
        withContext(dispatcher) {
            db.clothingItemQueries.getById(id).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun getByCategory(category: Category): Result<List<ClothingItem>> =
        runCatching {
            withContext(dispatcher) {
                db.clothingItemQueries.getByCategory(category.name).executeAsList()
                    .map { it.toDomain() }
            }
        }

    override suspend fun search(query: String): Result<List<ClothingItem>> = runCatching {
        withContext(dispatcher) {
            val pattern = "%$query%"
            db.clothingItemQueries.search(pattern).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun addItem(
        imageBytes: ByteArray,
        name: String,
        category: Category,
        colors: List<String>,
        seasons: List<Season>,
    ): Result<ClothingItem> = runCatching {
        withContext(dispatcher) {
            val id = Uuid.random().toString()
            val fileName = "$id.jpg"
            val photoPath = fileStorage.write(fileName, imageBytes)
            val createdAt = Clock.System.now().toEpochMilliseconds()

            db.clothingItemQueries.insert(
                id = id,
                name = name,
                category = category.name,
                colors = colors,
                seasons = seasons.map { it.name },
                tags = emptyList(),
                description = null,
                photoPath = photoPath,
                createdAt = createdAt,
            )

            ClothingItem(
                id = id,
                name = name,
                category = category,
                colors = colors,
                seasons = seasons,
                photoPath = photoPath,
                createdAt = createdAt,
            )
        }
    }

    override suspend fun analyzeAndTag(itemId: String): Result<ClothingItem> = runCatching {
        withContext(dispatcher) {
            val item = findById(itemId) ?: error("Item not found: $itemId")
            val imageBytes = fileStorage.read(item.photoPath)
            val analysis = aiClient.analyzeImage(imageBytes)

            val updated = item.copy(
                description = analysis.description,
                category = analysis.suggestedCategory,
                colors = analysis.colors,
                seasons = analysis.seasons,
                tags = analysis.tags,
            )

            db.clothingItemQueries.update(
                name = updated.name,
                category = updated.category.name,
                colors = updated.colors,
                seasons = updated.seasons.map { it.name },
                tags = updated.tags,
                description = updated.description,
                photoPath = updated.photoPath,
                id = updated.id,
            )

            updated
        }
    }

    override suspend fun updateItem(item: ClothingItem): Result<ClothingItem> = runCatching {
        withContext(dispatcher) {
            db.clothingItemQueries.update(
                name = item.name,
                category = item.category.name,
                colors = item.colors,
                seasons = item.seasons.map { it.name },
                tags = item.tags,
                description = item.description,
                photoPath = item.photoPath,
                id = item.id,
            )
            item
        }
    }

    override suspend fun deleteItem(id: String): Result<Unit> = runCatching {
        withContext(dispatcher) {
            val item = findById(id) ?: return@withContext
            fileStorage.delete(item.photoPath)
            db.clothingItemQueries.delete(id)
        }
    }

    override suspend fun getGapRecommendations(): Result<List<GapRecommendation>> = runCatching {
        withContext(dispatcher) {
            val items = db.clothingItemQueries.getAll().executeAsList().map { it.toDomain() }
            aiClient.getGapRecommendations(items)
        }
    }

    override suspend fun analyzeProspectiveItem(imageBytes: ByteArray): Result<TryItResult> =
        runCatching {
            withContext(dispatcher) {
                val items = db.clothingItemQueries.getAll().executeAsList().map { it.toDomain() }
                aiClient.analyzeProspectiveItem(imageBytes, items)
            }
        }

    private suspend fun findById(id: String): ClothingItem? = withContext(dispatcher) {
        db.clothingItemQueries.getById(id).executeAsOneOrNull()?.toDomain()
    }
}

private fun DbClothingItem.toDomain(): ClothingItem = ClothingItem(
    id = id,
    name = name,
    category = Category.valueOf(category),
    colors = colors,
    seasons = seasons.mapNotNull { runCatching { Season.valueOf(it) }.getOrNull() },
    tags = tags,
    description = description,
    photoPath = photoPath,
    createdAt = createdAt,
)
