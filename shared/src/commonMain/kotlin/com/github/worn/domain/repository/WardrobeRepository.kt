package com.github.worn.domain.repository

import com.github.worn.domain.model.AiAnalysisResult
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.TryItResult

interface WardrobeRepository {
    suspend fun getAll(): List<ClothingItem>
    suspend fun getById(id: String): ClothingItem?
    suspend fun getByCategory(category: Category): List<ClothingItem>
    suspend fun search(query: String): List<ClothingItem>
    suspend fun addItem(
        imageBytes: ByteArray,
        name: String,
        category: Category,
        colors: List<String>,
        seasons: List<Season>,
    ): ClothingItem
    suspend fun analyzeAndTag(itemId: String): ClothingItem
    suspend fun updateItem(item: ClothingItem): ClothingItem
    suspend fun deleteItem(id: String)
    suspend fun getGapRecommendations(): List<GapRecommendation>
    suspend fun analyzeProspectiveItem(imageBytes: ByteArray): TryItResult
}
