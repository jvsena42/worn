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
import kotlinx.datetime.Clock

class FakeWardrobeRepository : WardrobeRepository {

    val items = mutableListOf<ClothingItem>()

    var getAllError: Throwable? = null
    var getByCategoryError: Throwable? = null
    var addItemError: Throwable? = null
    var deleteItemError: Throwable? = null
    var updateItemError: Throwable? = null

    val deletedIds = mutableListOf<String>()

    override suspend fun getAll(): Result<List<ClothingItem>> =
        getAllError?.let { Result.failure(it) } ?: Result.success(items.toList())

    override suspend fun getById(id: String): Result<ClothingItem?> =
        Result.success(items.find { it.id == id })

    override suspend fun getByCategory(category: Category): Result<List<ClothingItem>> =
        getByCategoryError?.let { Result.failure(it) }
            ?: Result.success(items.filter { it.category == category })

    override suspend fun search(query: String): Result<List<ClothingItem>> =
        Result.success(items.filter { it.name.contains(query, ignoreCase = true) })

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
            id = "fake-${items.size + 1}",
            name = name,
            category = category,
            colors = colors,
            seasons = seasons,
            photoPath = "/photos/fake.jpg",
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        items.add(item)
        return Result.success(item)
    }

    override suspend fun analyzeAndTag(itemId: String): Result<ClothingItem> =
        Result.success(items.first { it.id == itemId })

    override suspend fun updateItem(item: ClothingItem): Result<ClothingItem> =
        updateItemError?.let { Result.failure(it) } ?: Result.success(item)

    override suspend fun deleteItem(id: String): Result<Unit> {
        deleteItemError?.let { return Result.failure(it) }
        deletedIds.add(id)
        items.removeAll { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun getGapRecommendations(): Result<List<GapRecommendation>> =
        Result.success(emptyList())

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
