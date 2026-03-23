package com.github.worn.repository

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransactionWithoutReturn
import com.github.worn.data.repository.WardrobeRepositoryImpl
import com.github.worn.data.source.local.PhotoFileStorage
import com.github.worn.data.source.local.db.ClothingItemQueries
import com.github.worn.data.source.local.db.OutfitItemQueries
import com.github.worn.data.source.local.db.OutfitQueries
import com.github.worn.data.source.local.db.WardrobeDatabase
import com.github.worn.data.source.remote.ClaudeApiClient
import com.github.worn.domain.model.AiAnalysisResult
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.TryItResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.github.worn.data.source.local.db.ClothingItem as DbClothingItem

@OptIn(ExperimentalCoroutinesApi::class)
class WardrobeRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val db = mockk<WardrobeDatabase>()
    private val queries = mockk<ClothingItemQueries>(relaxed = true)
    private val outfitItemQueries = mockk<OutfitItemQueries>(relaxed = true)
    private val outfitQueries = mockk<OutfitQueries>(relaxed = true)
    private val fileStorage = mockk<PhotoFileStorage>()
    private val aiClient = mockk<ClaudeApiClient>()

    private lateinit var repository: WardrobeRepositoryImpl

    private val dbItem = DbClothingItem(
        id = "item-1",
        name = "Blue T-Shirt",
        category = "TOP",
        colors = listOf("blue"),
        seasons = listOf("SUMMER"),
        tags = listOf("casual"),
        description = "A blue t-shirt",
        photoPath = "/photos/item-1.jpg",
        createdAt = 1_000_000L,
    )

    @BeforeTest
    fun setup() {
        every { db.clothingItemQueries } returns queries
        every { db.outfitItemQueries } returns outfitItemQueries
        every { db.outfitQueries } returns outfitQueries
        every { db.transaction(any(), any<TransactionWithoutReturn.() -> Unit>()) } answers {
            val body = arg<TransactionWithoutReturn.() -> Unit>(1)
            val tx = mockk<TransactionWithoutReturn>(relaxed = true)
            body(tx)
        }
        repository = WardrobeRepositoryImpl(db, fileStorage, aiClient, testDispatcher)
    }

    // region getAll

    @Test
    fun `getAll returns mapped domain items`() = runTest {
        val query = mockk<Query<DbClothingItem>>()
        every { queries.getAll() } returns query
        every { query.executeAsList() } returns listOf(dbItem)

        val result = repository.getAll()

        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertEquals(1, items.size)
        assertEquals("item-1", items[0].id)
        assertEquals(Category.TOP, items[0].category)
        assertEquals(listOf(Season.SUMMER), items[0].seasons)
    }

    // endregion

    // region getById

    @Test
    fun `getById returns item when exists`() = runTest {
        val query = mockk<Query<DbClothingItem>>()
        every { queries.getById("item-1") } returns query
        every { query.executeAsOneOrNull() } returns dbItem

        val result = repository.getById("item-1")

        assertTrue(result.isSuccess)
        val item = result.getOrThrow()
        assertEquals("item-1", item?.id)
        assertEquals(Category.TOP, item?.category)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        val query = mockk<Query<DbClothingItem>>()
        every { queries.getById("missing") } returns query
        every { query.executeAsOneOrNull() } returns null

        val result = repository.getById("missing")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    // endregion

    // region getByCategory

    @Test
    fun `getByCategory calls queries with category name`() = runTest {
        val query = mockk<Query<DbClothingItem>>()
        every { queries.getByCategory("TOP") } returns query
        every { query.executeAsList() } returns listOf(dbItem)

        val result = repository.getByCategory(Category.TOP)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        verify { queries.getByCategory("TOP") }
    }

    // endregion

    // region search

    @Test
    fun `search wraps query in wildcards`() = runTest {
        val query = mockk<Query<DbClothingItem>>()
        every { queries.search("%blue%") } returns query
        every { query.executeAsList() } returns listOf(dbItem)

        val result = repository.search("blue")

        assertTrue(result.isSuccess)
        verify { queries.search("%blue%") }
    }

    // endregion

    // region addItem

    @Test
    fun `addItem writes photo inserts DB and returns item`() = runTest {
        coEvery { fileStorage.write(any(), any()) } returns "/photos/new.jpg"

        val result = repository.addItem(
            imageBytes = byteArrayOf(1, 2, 3),
            name = "Red Jacket",
            category = Category.OUTERWEAR,
            colors = listOf("red"),
            seasons = listOf(Season.WINTER),
        )

        assertTrue(result.isSuccess)
        val item = result.getOrThrow()
        assertEquals("Red Jacket", item.name)
        assertEquals(Category.OUTERWEAR, item.category)
        assertEquals("/photos/new.jpg", item.photoPath)

        coVerify { fileStorage.write(any(), byteArrayOf(1, 2, 3)) }
        verify {
            queries.insert(
                id = any(),
                name = "Red Jacket",
                category = "OUTERWEAR",
                colors = listOf("red"),
                seasons = listOf("WINTER"),
                tags = emptyList(),
                description = null,
                photoPath = "/photos/new.jpg",
                createdAt = any(),
            )
        }
    }

    @Test
    fun `addItem wraps file write failure in Result failure`() = runTest {
        coEvery { fileStorage.write(any(), any()) } throws RuntimeException("Disk full")

        val result = repository.addItem(
            imageBytes = byteArrayOf(1),
            name = "Shirt",
            category = Category.TOP,
            colors = emptyList(),
            seasons = emptyList(),
        )

        assertTrue(result.isFailure)
        assertEquals("Disk full", result.exceptionOrNull()?.message)
    }

    // endregion

    // region analyzeAndTag

    @Test
    fun `analyzeAndTag reads photo calls AI and updates DB`() = runTest {
        val query = mockk<Query<DbClothingItem>>()
        every { queries.getById("item-1") } returns query
        every { query.executeAsOneOrNull() } returns dbItem
        coEvery { fileStorage.read("/photos/item-1.jpg") } returns byteArrayOf(10, 20)
        coEvery { aiClient.analyzeImage(byteArrayOf(10, 20)) } returns AiAnalysisResult(
            description = "Analyzed shirt",
            suggestedCategory = Category.TOP,
            colors = listOf("navy"),
            seasons = listOf(Season.SPRING, Season.FALL),
            tags = listOf("smart-casual"),
        )

        val result = repository.analyzeAndTag("item-1")

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals("Analyzed shirt", updated.description)
        assertEquals(listOf("navy"), updated.colors)
        assertEquals(listOf(Season.SPRING, Season.FALL), updated.seasons)
        assertEquals(listOf("smart-casual"), updated.tags)

        coVerify { fileStorage.read("/photos/item-1.jpg") }
        coVerify { aiClient.analyzeImage(byteArrayOf(10, 20)) }
        verify {
            queries.update(
                name = "Blue T-Shirt",
                category = "TOP",
                colors = listOf("navy"),
                seasons = listOf("SPRING", "FALL"),
                tags = listOf("smart-casual"),
                description = "Analyzed shirt",
                photoPath = "/photos/item-1.jpg",
                id = "item-1",
            )
        }
    }

    @Test
    fun `analyzeAndTag fails when item not found`() = runTest {
        val query = mockk<Query<DbClothingItem>>()
        every { queries.getById("missing") } returns query
        every { query.executeAsOneOrNull() } returns null

        val result = repository.analyzeAndTag("missing")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Item not found") == true)
    }

    // endregion

    // region deleteItem

    @Test
    fun `deleteItem removes photo and DB record and affected outfits`() = runTest {
        val query = mockk<Query<DbClothingItem>>()
        every { queries.getById("item-1") } returns query
        every { query.executeAsOneOrNull() } returns dbItem
        val outfitIdsQuery = mockk<Query<String>>()
        every { outfitItemQueries.getOutfitIdsForItem("item-1") } returns outfitIdsQuery
        every { outfitIdsQuery.executeAsList() } returns listOf("outfit-1")
        coEvery { fileStorage.delete("/photos/item-1.jpg") } returns Unit

        val result = repository.deleteItem("item-1")

        assertTrue(result.isSuccess)
        verify { outfitQueries.delete("outfit-1") }
        verify { queries.delete("item-1") }
        coVerify { fileStorage.delete("/photos/item-1.jpg") }
    }

    @Test
    fun `deleteItem succeeds when item not found`() = runTest {
        val query = mockk<Query<DbClothingItem>>()
        every { queries.getById("missing") } returns query
        every { query.executeAsOneOrNull() } returns null

        val result = repository.deleteItem("missing")

        assertTrue(result.isSuccess)
    }

    // endregion

    // region updateItem

    @Test
    fun `updateItem updates DB record`() = runTest {
        val item = com.github.worn.domain.model.ClothingItem(
            id = "item-1",
            name = "Updated Shirt",
            category = Category.TOP,
            colors = listOf("green"),
            seasons = listOf(Season.SPRING),
            tags = listOf("updated"),
            description = "Updated description",
            photoPath = "/photos/item-1.jpg",
            createdAt = 1_000_000L,
        )

        val result = repository.updateItem(item)

        assertTrue(result.isSuccess)
        assertEquals(item, result.getOrThrow())
        verify {
            queries.update(
                name = "Updated Shirt",
                category = "TOP",
                colors = listOf("green"),
                seasons = listOf("SPRING"),
                tags = listOf("updated"),
                description = "Updated description",
                photoPath = "/photos/item-1.jpg",
                id = "item-1",
            )
        }
    }

    // endregion
}
