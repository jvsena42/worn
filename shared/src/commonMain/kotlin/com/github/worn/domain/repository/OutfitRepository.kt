package com.github.worn.domain.repository

import com.github.worn.domain.model.Outfit
import kotlinx.coroutines.flow.Flow

interface OutfitRepository {
    /**
     * Saved outfits as a reactive stream. See [WardrobeRepository.observeAll] for why reads that
     * feed the UI are modelled as a `Flow` rather than a one-shot suspend call.
     */
    fun observeAll(): Flow<List<Outfit>>

    suspend fun getAll(): Result<List<Outfit>>
    suspend fun getById(id: String): Result<Outfit?>
    suspend fun createOutfit(name: String, itemIds: List<String>): Result<Outfit>
    suspend fun updateOutfit(outfit: Outfit): Result<Outfit>
    suspend fun deleteOutfit(id: String): Result<Unit>
}
