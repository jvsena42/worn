package com.github.worn.domain.repository

import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.TryItResult
import kotlinx.coroutines.flow.Flow

interface WardrobeRepository {
    /**
     * The wardrobe as a reactive stream, re-emitting whenever the underlying table changes.
     *
     * This is the single source of truth for anything that displays the wardrobe: callers never
     * need to re-query after a mutation, and re-entering a screen costs nothing because the
     * latest value is already cached in the ViewModel's [kotlinx.coroutines.flow.StateFlow].
     *
     * Unlike the one-shot reads below it is neither `suspend` (a cold Flow is returned
     * immediately) nor `Result`-wrapped — failures surface as an exception in the stream and are
     * handled with `catch` by the collector.
     */
    fun observeAll(): Flow<List<ClothingItem>>

    suspend fun getAll(): Result<List<ClothingItem>>
    suspend fun getById(id: String): Result<ClothingItem?>
    suspend fun getByCategory(category: Category): Result<List<ClothingItem>>
    suspend fun search(query: String): Result<List<ClothingItem>>
    suspend fun addItem(
        imageBytes: ByteArray,
        name: String,
        category: Category,
        colors: List<String>,
        seasons: List<Season>,
        subcategory: Subcategory? = null,
        fit: Fit? = null,
        material: Material? = null,
    ): Result<ClothingItem>
    suspend fun analyzeAndTag(itemId: String): Result<ClothingItem>
    suspend fun updateItem(item: ClothingItem): Result<ClothingItem>
    suspend fun deleteItem(id: String): Result<Unit>
    suspend fun getGapRecommendations(): Result<List<GapRecommendation>>
    suspend fun analyzeProspectiveItem(imageBytes: ByteArray): Result<TryItResult>
}
