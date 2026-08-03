package com.github.worn.fake

import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.TryItResult
import com.github.worn.domain.repository.WardrobeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

class FakeWardrobeRepository : WardrobeRepository {

    /** Assigning to `value` emits on [observeAll], as a real write would. */
    val items = MutableStateFlow<List<ClothingItem>>(emptyList())

    var getAllError: Throwable? = null
    var getByCategoryError: Throwable? = null
    var addItemError: Throwable? = null
    var deleteItemError: Throwable? = null
    var updateItemError: Throwable? = null
    var observeAllError: Throwable? = null

    var gapRecommendations: List<GapRecommendation> = emptyList()
    var gapRecommendationsError: Throwable? = null

    /** Lets tests assert that a wardrobe write never triggers a new (paid) AI call. */
    var gapRecommendationCalls = 0
        private set

    val deletedIds = mutableListOf<String>()

    fun addItems(vararg newItems: ClothingItem) {
        items.value = items.value + newItems
    }

    override fun observeAll(): Flow<List<ClothingItem>> =
        observeAllError?.let { error -> flow { throw error } } ?: items

    override suspend fun getAll(): Result<List<ClothingItem>> =
        getAllError?.let { Result.failure(it) } ?: Result.success(items.value)

    override suspend fun getById(id: String): Result<ClothingItem?> =
        Result.success(items.value.find { it.id == id })

    override suspend fun getByCategory(category: Category): Result<List<ClothingItem>> =
        getByCategoryError?.let { Result.failure(it) }
            ?: Result.success(items.value.filter { it.category == category })

    override suspend fun search(query: String): Result<List<ClothingItem>> =
        Result.success(items.value.filter { it.name.contains(query, ignoreCase = true) })

    override suspend fun addItem(
        imageBytes: ByteArray,
        name: String,
        category: Category,
        colors: List<String>,
        seasons: List<Season>,
        subcategory: Subcategory?,
        fit: Fit?,
        material: Material?,
    ): Result<ClothingItem> {
        addItemError?.let { return Result.failure(it) }
        val item = ClothingItem(
            id = "fake-${items.value.size + 1}",
            name = name,
            category = category,
            colors = colors,
            seasons = seasons,
            subcategory = subcategory,
            fit = fit,
            material = material,
            photoPath = "/photos/fake.jpg",
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        items.value = items.value + item
        return Result.success(item)
    }

    override suspend fun analyzeAndTag(itemId: String): Result<ClothingItem> =
        Result.success(items.value.first { it.id == itemId })

    override suspend fun updateItem(item: ClothingItem): Result<ClothingItem> =
        updateItemError?.let { Result.failure(it) } ?: Result.success(item)

    override suspend fun deleteItem(id: String): Result<Unit> {
        deleteItemError?.let { return Result.failure(it) }
        deletedIds.add(id)
        items.value = items.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun getGapRecommendations(): Result<List<GapRecommendation>> {
        gapRecommendationCalls++
        return gapRecommendationsError?.let { Result.failure(it) } ?: Result.success(gapRecommendations)
    }

    override suspend fun analyzeProspectiveItem(imageBytes: ByteArray): Result<TryItResult> =
        Result.success(
            TryItResult(
                matchingItems = emptyList(),
                combinationsUnlocked = 0,
                gapsFilled = emptyList(),
                worthAdding = false,
            ),
        )
}
