package com.github.worn.viewmodel

import app.cash.turbine.test
import com.github.worn.domain.model.Category
import com.github.worn.fake.FakeOutfitRepository
import com.github.worn.fake.FakeWardrobeRepository
import com.github.worn.fake.clothingItem
import com.github.worn.fake.outfit
import com.github.worn.presentation.viewmodel.OutfitEffect
import com.github.worn.presentation.viewmodel.OutfitIntent
import com.github.worn.presentation.viewmodel.OutfitViewModel
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
class OutfitViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var outfitRepository: FakeOutfitRepository
    private lateinit var wardrobeRepository: FakeWardrobeRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        outfitRepository = FakeOutfitRepository()
        wardrobeRepository = FakeWardrobeRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OutfitViewModel =
        OutfitViewModel(outfitRepository, wardrobeRepository)

    // region init

    @Test
    fun `init loads outfits on creation`() {
        val o = outfit(id = "1")
        outfitRepository.outfits.add(o)

        val vm = createViewModel()

        assertEquals(listOf(o), vm.state.value.outfits)
        assertFalse(vm.state.value.isLoading)
    }

    // endregion

    // region LoadOutfits

    @Test
    fun `LoadOutfits success updates state with outfits`() {
        val vm = createViewModel()

        val o = outfit(id = "2")
        outfitRepository.outfits.add(o)
        vm.onIntent(OutfitIntent.LoadOutfits)

        assertEquals(listOf(o), vm.state.value.outfits)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `LoadOutfits failure sets error and sends ShowError effect`() = runTest {
        val vm = createViewModel()

        outfitRepository.getAllError = RuntimeException("DB error")
        vm.effects.test {
            vm.onIntent(OutfitIntent.LoadOutfits)

            val effect = awaitItem()
            assertIs<OutfitEffect.ShowError>(effect)
            assertEquals("DB error", effect.message)
        }
        assertEquals("DB error", vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    // endregion

    // region LoadClothingItems

    @Test
    fun `LoadClothingItems success updates state with items`() {
        val item = clothingItem(id = "1")
        wardrobeRepository.items.add(item)
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.LoadClothingItems)

        assertEquals(listOf(item), vm.state.value.clothingItems)
        assertFalse(vm.state.value.isLoadingItems)
    }

    @Test
    fun `FilterItemsByCategory filters items and updates activeCategory`() {
        val top = clothingItem(id = "1", category = Category.TOP)
        val bottom = clothingItem(id = "2", category = Category.BOTTOM)
        wardrobeRepository.items.addAll(listOf(top, bottom))
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.FilterItemsByCategory(Category.TOP))

        assertEquals(Category.TOP, vm.state.value.activeItemCategory)
        assertEquals(listOf(top), vm.state.value.clothingItems)
    }

    // endregion

    // region ToggleItemSelection

    @Test
    fun `ToggleItemSelection adds item to selectedItemIds`() {
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleItemSelection("item-1"))

        assertTrue("item-1" in vm.state.value.selectedItemIds)
    }

    @Test
    fun `ToggleItemSelection removes already-selected item`() {
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleItemSelection("item-1"))
        vm.onIntent(OutfitIntent.ToggleItemSelection("item-1"))

        assertTrue(vm.state.value.selectedItemIds.isEmpty())
    }

    // endregion

    // region ToggleSelection / ClearSelection

    @Test
    fun `ToggleSelection adds outfit to selectedIds`() {
        val o = outfit(id = "1")
        outfitRepository.outfits.add(o)
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))

        assertTrue("1" in vm.state.value.selectedIds)
    }

    @Test
    fun `ToggleSelection removes already-selected outfit`() {
        val o = outfit(id = "1")
        outfitRepository.outfits.add(o)
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))
        vm.onIntent(OutfitIntent.ToggleSelection("1"))

        assertTrue(vm.state.value.selectedIds.isEmpty())
    }

    @Test
    fun `ClearSelection empties selectedIds`() {
        val o = outfit(id = "1")
        outfitRepository.outfits.add(o)
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))
        vm.onIntent(OutfitIntent.ClearSelection)

        assertTrue(vm.state.value.selectedIds.isEmpty())
    }

    // endregion

    // region CreateOutfit

    @Test
    fun `CreateOutfit success sends OutfitCreated and reloads`() = runTest {
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleItemSelection("item-1"))
        vm.onIntent(OutfitIntent.ToggleItemSelection("item-2"))

        vm.effects.test {
            vm.onIntent(OutfitIntent.CreateOutfit("Weekend Casual"))

            val effect = awaitItem()
            assertIs<OutfitEffect.OutfitCreated>(effect)
        }
        assertFalse(vm.state.value.isSaving)
        assertTrue(vm.state.value.selectedItemIds.isEmpty())
        assertEquals(1, outfitRepository.outfits.size)
        assertEquals("Weekend Casual", outfitRepository.outfits.first().name)
    }

    @Test
    fun `CreateOutfit failure sends ShowError`() = runTest {
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleItemSelection("item-1"))
        outfitRepository.createOutfitError = RuntimeException("DB full")

        vm.effects.test {
            vm.onIntent(OutfitIntent.CreateOutfit("Test"))

            val effect = awaitItem()
            assertIs<OutfitEffect.ShowError>(effect)
            assertEquals("DB full", effect.message)
        }
        assertFalse(vm.state.value.isSaving)
    }

    // endregion

    // region DeleteSelected

    @Test
    fun `DeleteSelected removes outfits and sends OutfitsDeleted`() = runTest {
        val o1 = outfit(id = "1")
        val o2 = outfit(id = "2")
        outfitRepository.outfits.addAll(listOf(o1, o2))
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))

        vm.effects.test {
            vm.onIntent(OutfitIntent.DeleteSelected)

            val effect = awaitItem()
            assertIs<OutfitEffect.OutfitsDeleted>(effect)
        }
        assertTrue(vm.state.value.selectedIds.isEmpty())
        assertFalse(vm.state.value.isDeleting)
        assertEquals(listOf("1"), outfitRepository.deletedIds)
    }

    @Test
    fun `DeleteSelected partial failure sends ShowError`() = runTest {
        val o1 = outfit(id = "1")
        val o2 = outfit(id = "2")
        outfitRepository.outfits.addAll(listOf(o1, o2))
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))
        vm.onIntent(OutfitIntent.ToggleSelection("2"))
        outfitRepository.deleteOutfitError = RuntimeException("Failed")

        vm.effects.test {
            vm.onIntent(OutfitIntent.DeleteSelected)

            val effect = awaitItem()
            assertIs<OutfitEffect.ShowError>(effect)
            assertEquals("Some outfits could not be deleted", effect.message)
        }
        assertFalse(vm.state.value.isDeleting)
    }

    // endregion
}
