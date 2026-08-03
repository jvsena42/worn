package com.github.worn.repository

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransactionWithoutReturn
import com.github.worn.data.repository.OutfitRepositoryImpl
import com.github.worn.data.source.local.db.ClothingItemQueries
import com.github.worn.data.source.local.db.OutfitItemQueries
import com.github.worn.data.source.local.db.OutfitQueries
import com.github.worn.data.source.local.db.WardrobeDatabase
import com.github.worn.domain.model.Outfit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OutfitRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val db = mockk<WardrobeDatabase>()
    private val outfitQueries = mockk<OutfitQueries>(relaxed = true)
    private val outfitItemQueries = mockk<OutfitItemQueries>(relaxed = true)
    private val clothingItemQueries = mockk<ClothingItemQueries>(relaxed = true)

    private lateinit var repository: OutfitRepositoryImpl

    @BeforeTest
    fun setup() {
        every { db.outfitQueries } returns outfitQueries
        every { db.outfitItemQueries } returns outfitItemQueries
        every { db.clothingItemQueries } returns clothingItemQueries
        every { db.transaction(any(), any<TransactionWithoutReturn.() -> Unit>()) } answers {
            val body = arg<TransactionWithoutReturn.() -> Unit>(1)
            val tx = mockk<TransactionWithoutReturn>(relaxed = true)
            body(tx)
        }
        repository = OutfitRepositoryImpl(db, testDispatcher)
    }

    /**
     * Stands in for `getNamesByIds`, which SQLDelight generates with a caller-supplied mapper:
     * the mapper is captured and applied to [rows] so the test asserts on the repository's
     * ordering, not on SQLDelight's row plumbing.
     */
    private fun stubItemNames(vararg rows: Pair<String, String>) {
        every { clothingItemQueries.getNamesByIds(any(), any<(String, String) -> Any>()) } answers {
            val ids = arg<Collection<String>>(0).toSet()
            @Suppress("UNCHECKED_CAST")
            val mapper = arg<(String, String) -> Any>(1) as (String, String) -> Pair<String, String>
            mockk<Query<Pair<String, String>>> {
                every { executeAsList() } returns
                    rows.filter { it.first in ids }.map { mapper(it.first, it.second) }
            }
        }
    }

    // region createOutfit

    @Test
    fun `createOutfit keeps the provided name`() = runTest {
        val result = repository.createOutfit(name = "Weekend Casual", itemIds = listOf("item-1"))

        assertTrue(result.isSuccess)
        assertEquals("Weekend Casual", result.getOrThrow().name)
        verify { outfitQueries.insert(any(), "Weekend Casual", any()) }
        verify(exactly = 0) { clothingItemQueries.getNamesByIds(any(), any<(String, String) -> Any>()) }
    }

    @Test
    fun `createOutfit falls back to item names joined by plus`() = runTest {
        stubItemNames("item-1" to "Black T-Shirt", "item-2" to "Navy Jeans")

        val result = repository.createOutfit(name = "", itemIds = listOf("item-1", "item-2"))

        assertTrue(result.isSuccess)
        assertEquals("Black T-Shirt + Navy Jeans", result.getOrThrow().name)
        verify { outfitQueries.insert(any(), "Black T-Shirt + Navy Jeans", any()) }
    }

    @Test
    fun `createOutfit default name follows the selection order`() = runTest {
        stubItemNames("item-1" to "Black T-Shirt", "item-2" to "Navy Jeans")

        val result = repository.createOutfit(name = "   ", itemIds = listOf("item-2", "item-1"))

        assertEquals("Navy Jeans + Black T-Shirt", result.getOrThrow().name)
    }

    @Test
    fun `createOutfit default name skips ids with no matching item`() = runTest {
        stubItemNames("item-1" to "Black T-Shirt")

        val result = repository.createOutfit(name = "", itemIds = listOf("item-1", "missing"))

        assertEquals("Black T-Shirt", result.getOrThrow().name)
    }

    // endregion

    // region updateOutfit

    @Test
    fun `updateOutfit falls back to item names when the name is cleared`() = runTest {
        stubItemNames("item-1" to "Black T-Shirt", "item-2" to "Navy Jeans")
        val outfit = Outfit(id = "o-1", name = "", itemIds = listOf("item-1", "item-2"), createdAt = 0)

        val result = repository.updateOutfit(outfit)

        assertTrue(result.isSuccess)
        assertEquals("Black T-Shirt + Navy Jeans", result.getOrThrow().name)
        verify { outfitQueries.update("Black T-Shirt + Navy Jeans", "o-1") }
    }

    @Test
    fun `updateOutfit keeps the provided name`() = runTest {
        val outfit = Outfit(id = "o-1", name = "Weekend Casual", itemIds = listOf("item-1"), createdAt = 0)

        val result = repository.updateOutfit(outfit)

        assertEquals("Weekend Casual", result.getOrThrow().name)
        verify { outfitQueries.update("Weekend Casual", "o-1") }
    }

    // endregion
}
