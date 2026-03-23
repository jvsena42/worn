package com.github.worn.fake

import com.github.worn.domain.model.Outfit
import com.github.worn.domain.repository.OutfitRepository
import kotlinx.datetime.Clock

class FakeOutfitRepository : OutfitRepository {

    val outfits = mutableListOf<Outfit>()

    var getAllError: Throwable? = null
    var deleteOutfitError: Throwable? = null

    val deletedIds = mutableListOf<String>()

    override suspend fun getAll(): Result<List<Outfit>> =
        getAllError?.let { Result.failure(it) } ?: Result.success(outfits.toList())

    override suspend fun getById(id: String): Result<Outfit?> =
        Result.success(outfits.find { it.id == id })

    override suspend fun createOutfit(name: String, itemIds: List<String>): Result<Outfit> {
        val outfit = Outfit(
            id = "fake-${outfits.size + 1}",
            name = name,
            itemIds = itemIds,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        outfits.add(outfit)
        return Result.success(outfit)
    }

    override suspend fun updateOutfit(outfit: Outfit): Result<Outfit> =
        Result.success(outfit)

    override suspend fun deleteOutfit(id: String): Result<Unit> {
        deleteOutfitError?.let { return Result.failure(it) }
        deletedIds.add(id)
        outfits.removeAll { it.id == id }
        return Result.success(Unit)
    }
}
