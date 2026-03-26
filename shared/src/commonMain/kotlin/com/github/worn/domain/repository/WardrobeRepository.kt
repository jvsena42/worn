package com.github.worn.domain.repository

import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.TryItResult

interface WardrobeRepository {
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
