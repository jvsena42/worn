package com.github.worn.domain.repository

import com.github.worn.domain.model.Outfit

interface OutfitRepository {
    suspend fun getAll(): Result<List<Outfit>>
    suspend fun getById(id: String): Result<Outfit?>
    suspend fun createOutfit(name: String, itemIds: List<String>): Result<Outfit>
    suspend fun updateOutfit(outfit: Outfit): Result<Outfit>
    suspend fun deleteOutfit(id: String): Result<Unit>
}
