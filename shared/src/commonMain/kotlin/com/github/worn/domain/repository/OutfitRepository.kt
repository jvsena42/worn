package com.github.worn.domain.repository

import com.github.worn.domain.model.Outfit

interface OutfitRepository {
    suspend fun getAll(): List<Outfit>
    suspend fun getById(id: String): Outfit?
    suspend fun createOutfit(name: String, itemIds: List<String>): Outfit
    suspend fun updateOutfit(outfit: Outfit): Outfit
    suspend fun deleteOutfit(id: String)
}
