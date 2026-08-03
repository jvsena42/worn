package com.github.worn.data.repository

import com.github.worn.data.source.local.db.WardrobeDatabase
import com.github.worn.domain.model.Outfit
import com.github.worn.domain.repository.OutfitRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Joins item names into an outfit's default name, e.g. `Black T-Shirt + Navy Jeans`. */
private const val NAME_SEPARATOR = " + "

@OptIn(ExperimentalUuidApi::class)
class OutfitRepositoryImpl(
    private val db: WardrobeDatabase,
    private val dispatcher: CoroutineContext,
) : OutfitRepository {

    override fun observeAll(): Flow<List<Outfit>> =
        db.outfitQueries.getAllWithItems(::OutfitRow)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.toOutfits() }

    override suspend fun getAll(): Result<List<Outfit>> = runCatching {
        withContext(dispatcher) {
            db.outfitQueries.getAllWithItems(::OutfitRow).executeAsList().toOutfits()
        }
    }

    override suspend fun getById(id: String): Result<Outfit?> = runCatching {
        withContext(dispatcher) {
            db.outfitQueries.getByIdWithItems(id, ::OutfitRow).executeAsList()
                .toOutfits()
                .firstOrNull()
        }
    }

    override suspend fun createOutfit(name: String, itemIds: List<String>): Result<Outfit> =
        runCatching {
            withContext(dispatcher) {
                val id = Uuid.random().toString()
                val createdAt = Clock.System.now().toEpochMilliseconds()
                val resolvedName = resolveName(name, itemIds)

                db.transaction {
                    db.outfitQueries.insert(id = id, name = resolvedName, createdAt = createdAt)
                    itemIds.forEach { itemId ->
                        db.outfitItemQueries.insertItem(outfitId = id, itemId = itemId)
                    }
                }

                Outfit(id = id, name = resolvedName, itemIds = itemIds, createdAt = createdAt)
            }
        }

    override suspend fun updateOutfit(outfit: Outfit): Result<Outfit> = runCatching {
        withContext(dispatcher) {
            val resolved = outfit.copy(name = resolveName(outfit.name, outfit.itemIds))
            db.transaction {
                db.outfitQueries.update(name = resolved.name, id = resolved.id)
                db.outfitItemQueries.deleteAllForOutfit(resolved.id)
                resolved.itemIds.forEach { itemId ->
                    db.outfitItemQueries.insertItem(outfitId = resolved.id, itemId = itemId)
                }
            }
            resolved
        }
    }

    /**
     * Falls back to the outfit's item names joined by [NAME_SEPARATOR] when the user left the name
     * empty, so every outfit ends up with something readable in the list.
     */
    private fun resolveName(name: String, itemIds: List<String>): String =
        name.ifBlank { defaultName(itemIds) }.trim()

    private fun defaultName(itemIds: List<String>): String {
        if (itemIds.isEmpty()) return ""
        val namesById = db.clothingItemQueries
            .getNamesByIds(itemIds) { id, name -> id to name }
            .executeAsList()
            .toMap()
        return itemIds.mapNotNull { namesById[it] }.joinToString(NAME_SEPARATOR)
    }

    override suspend fun deleteOutfit(id: String): Result<Unit> = runCatching {
        withContext(dispatcher) {
            db.outfitQueries.delete(id)
        }
    }
}

/** One row of the outfit/outfitItem join; [itemId] is null for an outfit with no items. */
private data class OutfitRow(
    val id: String,
    val name: String,
    val createdAt: Long,
    val itemId: String?,
)

/** Regroups join rows into one [Outfit] per id, preserving the query's `createdAt DESC` order. */
private fun List<OutfitRow>.toOutfits(): List<Outfit> =
    groupBy { it.id }.map { (_, rows) ->
        val first = rows.first()
        Outfit(
            id = first.id,
            name = first.name,
            itemIds = rows.mapNotNull { it.itemId },
            createdAt = first.createdAt,
        )
    }
