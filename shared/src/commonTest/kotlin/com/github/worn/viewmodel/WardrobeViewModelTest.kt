package com.github.worn.viewmodel

import app.cash.turbine.test
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.Season
import com.github.worn.fake.FakeSecretStore
import com.github.worn.fake.FakeWardrobeRepository
import com.github.worn.fake.clothingItem
import com.github.worn.presentation.viewmodel.WardrobeEffect
import com.github.worn.presentation.viewmodel.WardrobeIntent
import com.github.worn.presentation.viewmodel.WardrobeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WardrobeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeWardrobeRepository
    private lateinit var secretStore: FakeSecretStore

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWardrobeRepository()
        secretStore = FakeSecretStore()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): WardrobeViewModel =
        WardrobeViewModel(repository, secretStore)

    // region init

    @Test
    fun `init sets hasApiKey true when key exists`() {
        secretStore.storedKey = "test-key"
        val vm = createViewModel()
        assertTrue(vm.state.value.hasApiKey)
    }

    @Test
    fun `init sets hasApiKey false when key is null`() {
        secretStore.storedKey = null
        val vm = createViewModel()
        assertFalse(vm.state.value.hasApiKey)
    }

    @Test
    fun `init loads items on creation`() {
        val item = clothingItem(id = "1")
        repository.items.add(item)

        val vm = createViewModel()

        assertEquals(listOf(item), vm.state.value.items)
        assertFalse(vm.state.value.isLoading)
    }

    // endregion

    // region LoadItems

    @Test
    fun `LoadItems success updates state with items`() {
        val vm = createViewModel()

        val item = clothingItem(id = "2")
        repository.items.add(item)
        vm.onIntent(WardrobeIntent.LoadItems)

        assertEquals(listOf(item), vm.state.value.items)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `LoadItems failure sets error and sends ShowError effect`() = runTest {
        val vm = createViewModel()

        repository.getAllError = RuntimeException("DB error")
        vm.effects.test {
            vm.onIntent(WardrobeIntent.LoadItems)

            val effect = awaitItem()
            assertIs<WardrobeEffect.ShowError>(effect)
            assertEquals("DB error", effect.message)
        }
        assertEquals("DB error", vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    // endregion

    // region FilterByCategory

    @Test
    fun `FilterByCategory updates activeCategory and filters items`() {
        val top = clothingItem(id = "1", category = Category.TOP)
        val bottom = clothingItem(id = "2", category = Category.BOTTOM)
        repository.items.addAll(listOf(top, bottom))

        val vm = createViewModel()
        vm.onIntent(WardrobeIntent.FilterByCategory(Category.TOP))

        assertEquals(Category.TOP, vm.state.value.activeCategory)
        assertEquals(listOf(top), vm.state.value.items)
        assertEquals(2, vm.state.value.totalItemCount)
    }

    @Test
    fun `FilterByCategory with null shows all items`() {
        val top = clothingItem(id = "1", category = Category.TOP)
        val bottom = clothingItem(id = "2", category = Category.BOTTOM)
        repository.items.addAll(listOf(top, bottom))

        val vm = createViewModel()
        vm.onIntent(WardrobeIntent.FilterByCategory(Category.TOP))
        vm.onIntent(WardrobeIntent.FilterByCategory(null))

        assertNull(vm.state.value.activeCategory)
        assertEquals(listOf(top, bottom), vm.state.value.items)
        assertEquals(2, vm.state.value.totalItemCount)
    }

    @Test
    fun `FilterByCategory to empty category keeps totalItemCount`() {
        val top = clothingItem(id = "1", category = Category.TOP)
        repository.items.add(top)

        val vm = createViewModel()
        vm.onIntent(WardrobeIntent.FilterByCategory(Category.SHOES))

        assertEquals(Category.SHOES, vm.state.value.activeCategory)
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(1, vm.state.value.totalItemCount)
    }

    // endregion

    // region AddItem

    @Test
    fun `AddItem success sends ItemAdded effect and reloads`() = runTest {
        val vm = createViewModel()

        vm.effects.test {
            vm.onIntent(
                WardrobeIntent.AddItem(
                    imageBytes = byteArrayOf(1, 2, 3),
                    name = "New Shirt",
                    category = Category.TOP,
                    colors = listOf("red"),
                    seasons = listOf(Season.SUMMER),
                ),
            )

            val effect = awaitItem()
            assertIs<WardrobeEffect.ItemAdded>(effect)
        }
        assertFalse(vm.state.value.isSaving)
        assertEquals(1, vm.state.value.items.size)
    }

    @Test
    fun `AddItem failure sends ShowError effect`() = runTest {
        val vm = createViewModel()
        repository.addItemError = RuntimeException("Storage full")

        vm.effects.test {
            vm.onIntent(
                WardrobeIntent.AddItem(
                    imageBytes = byteArrayOf(1, 2, 3),
                    name = "New Shirt",
                    category = Category.TOP,
                    colors = listOf("red"),
                    seasons = listOf(Season.SUMMER),
                ),
            )

            val effect = awaitItem()
            assertIs<WardrobeEffect.ShowError>(effect)
            assertEquals("Storage full", effect.message)
        }
        assertFalse(vm.state.value.isSaving)
    }

    // endregion

    // region ToggleSelection / ClearSelection

    @Test
    fun `ToggleSelection adds item to selectedIds`() {
        val item = clothingItem(id = "1")
        repository.items.add(item)
        val vm = createViewModel()

        vm.onIntent(WardrobeIntent.ToggleSelection("1"))

        assertTrue("1" in vm.state.value.selectedIds)
    }

    @Test
    fun `ToggleSelection removes already-selected item`() {
        val item = clothingItem(id = "1")
        repository.items.add(item)
        val vm = createViewModel()

        vm.onIntent(WardrobeIntent.ToggleSelection("1"))
        vm.onIntent(WardrobeIntent.ToggleSelection("1"))

        assertTrue(vm.state.value.selectedIds.isEmpty())
    }

    @Test
    fun `ClearSelection empties selectedIds`() {
        val item = clothingItem(id = "1")
        repository.items.add(item)
        val vm = createViewModel()

        vm.onIntent(WardrobeIntent.ToggleSelection("1"))
        vm.onIntent(WardrobeIntent.ClearSelection)

        assertTrue(vm.state.value.selectedIds.isEmpty())
    }

    // endregion

    // region DeleteSelected

    @Test
    fun `DeleteSelected removes items and sends ItemsDeleted`() = runTest {
        val item1 = clothingItem(id = "1")
        val item2 = clothingItem(id = "2")
        repository.items.addAll(listOf(item1, item2))
        val vm = createViewModel()

        vm.onIntent(WardrobeIntent.ToggleSelection("1"))

        vm.effects.test {
            vm.onIntent(WardrobeIntent.DeleteSelected)

            val effect = awaitItem()
            assertIs<WardrobeEffect.ItemsDeleted>(effect)
        }
        assertTrue(vm.state.value.selectedIds.isEmpty())
        assertFalse(vm.state.value.isDeleting)
        assertEquals(listOf("1"), repository.deletedIds)
    }

    @Test
    fun `DeleteSelected partial failure sends ShowError`() = runTest {
        val item1 = clothingItem(id = "1")
        val item2 = clothingItem(id = "2")
        repository.items.addAll(listOf(item1, item2))
        val vm = createViewModel()

        vm.onIntent(WardrobeIntent.ToggleSelection("1"))
        vm.onIntent(WardrobeIntent.ToggleSelection("2"))
        repository.deleteItemError = RuntimeException("Failed")

        vm.effects.test {
            vm.onIntent(WardrobeIntent.DeleteSelected)

            val effect = awaitItem()
            assertIs<WardrobeEffect.ShowError>(effect)
            assertEquals("Some items could not be deleted", effect.message)
        }
        assertFalse(vm.state.value.isDeleting)
    }

    // endregion
}
