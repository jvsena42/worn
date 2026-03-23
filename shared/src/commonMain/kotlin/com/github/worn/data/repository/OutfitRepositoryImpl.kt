package com.github.worn.data.repository

import com.github.worn.data.source.local.db.WardrobeDatabase
import com.github.worn.domain.model.Outfit
import com.github.worn.domain.repository.OutfitRepository
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class OutfitRepositoryImpl(
    private val db: WardrobeDatabase,
    private val dispatcher: CoroutineContext,
) : OutfitRepository {

    override suspend fun getAll(): Result<List<Outfit>> = runCatching {
        withContext(dispatcher) {
            db.outfitQueries.getAll().executeAsList().map { dbOutfit ->
                val itemIds = db.outfitItemQueries
                    .getItemIdsForOutfit(dbOutfit.id)
                    .executeAsList()
                Outfit(
                    id = dbOutfit.id,
                    name = dbOutfit.name,
                    itemIds = itemIds,
                    createdAt = dbOutfit.createdAt,
                )
            }
        }
    }

    override suspend fun getById(id: String): Result<Outfit?> = runCatching {
        withContext(dispatcher) {
            val dbOutfit = db.outfitQueries.getById(id).executeAsOneOrNull()
                ?: return@withContext null
            val itemIds = db.outfitItemQueries
                .getItemIdsForOutfit(dbOutfit.id)
                .executeAsList()
            Outfit(
                id = dbOutfit.id,
                name = dbOutfit.name,
                itemIds = itemIds,
                createdAt = dbOutfit.createdAt,
            )
        }
    }

    override suspend fun createOutfit(name: String, itemIds: List<String>): Result<Outfit> =
        runCatching {
            withContext(dispatcher) {
                val id = Uuid.random().toString()
                val createdAt = Clock.System.now().toEpochMilliseconds()

                db.transaction {
                    db.outfitQueries.insert(id = id, name = name, createdAt = createdAt)
                    itemIds.forEach { itemId ->
                        db.outfitItemQueries.insertItem(outfitId = id, itemId = itemId)
                    }
                }

                Outfit(id = id, name = name, itemIds = itemIds, createdAt = createdAt)
            }
        }

    override suspend fun updateOutfit(outfit: Outfit): Result<Outfit> = runCatching {
        withContext(dispatcher) {
            db.transaction {
                db.outfitQueries.update(name = outfit.name, id = outfit.id)
                db.outfitItemQueries.deleteAllForOutfit(outfit.id)
                outfit.itemIds.forEach { itemId ->
                    db.outfitItemQueries.insertItem(outfitId = outfit.id, itemId = itemId)
                }
            }
            outfit
        }
    }

    override suspend fun deleteOutfit(id: String): Result<Unit> = runCatching {
        withContext(dispatcher) {
            db.outfitQueries.delete(id)
        }
    }
}
