package com.github.worn.fake

import com.github.worn.domain.model.Outfit
import com.github.worn.domain.repository.OutfitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

class FakeOutfitRepository : OutfitRepository {

    /** Stands in for the outfit table; assigning to `value` emits on [observeAll]. */
    val outfits = MutableStateFlow<List<Outfit>>(emptyList())

    var getAllError: Throwable? = null
    var createOutfitError: Throwable? = null
    var deleteOutfitError: Throwable? = null
    var observeAllError: Throwable? = null

    val deletedIds = mutableListOf<String>()

    fun addOutfits(vararg newOutfits: Outfit) {
        outfits.value = outfits.value + newOutfits
    }

    override fun observeAll(): Flow<List<Outfit>> =
        observeAllError?.let { error -> flow { throw error } } ?: outfits

    override suspend fun getAll(): Result<List<Outfit>> =
        getAllError?.let { Result.failure(it) } ?: Result.success(outfits.value)

    override suspend fun getById(id: String): Result<Outfit?> =
        Result.success(outfits.value.find { it.id == id })

    override suspend fun createOutfit(name: String, itemIds: List<String>): Result<Outfit> {
        createOutfitError?.let { return Result.failure(it) }
        val outfit = Outfit(
            id = "fake-${outfits.value.size + 1}",
            name = name,
            itemIds = itemIds,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        outfits.value = outfits.value + outfit
        return Result.success(outfit)
    }

    override suspend fun updateOutfit(outfit: Outfit): Result<Outfit> =
        Result.success(outfit)

    override suspend fun deleteOutfit(id: String): Result<Unit> {
        deleteOutfitError?.let { return Result.failure(it) }
        deletedIds.add(id)
        outfits.value = outfits.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}
