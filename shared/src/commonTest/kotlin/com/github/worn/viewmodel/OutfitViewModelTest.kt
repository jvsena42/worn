package com.github.worn.viewmodel

import app.cash.turbine.test
import com.github.worn.fake.FakeOutfitRepository
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
    private lateinit var repository: FakeOutfitRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeOutfitRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OutfitViewModel =
        OutfitViewModel(repository)

    // region init

    @Test
    fun `init loads outfits on creation`() {
        val o = outfit(id = "1")
        repository.outfits.add(o)

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
        repository.outfits.add(o)
        vm.onIntent(OutfitIntent.LoadOutfits)

        assertEquals(listOf(o), vm.state.value.outfits)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `LoadOutfits failure sets error and sends ShowError effect`() = runTest {
        val vm = createViewModel()

        repository.getAllError = RuntimeException("DB error")
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

    // region ToggleSelection / ClearSelection

    @Test
    fun `ToggleSelection adds outfit to selectedIds`() {
        val o = outfit(id = "1")
        repository.outfits.add(o)
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))

        assertTrue("1" in vm.state.value.selectedIds)
    }

    @Test
    fun `ToggleSelection removes already-selected outfit`() {
        val o = outfit(id = "1")
        repository.outfits.add(o)
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))
        vm.onIntent(OutfitIntent.ToggleSelection("1"))

        assertTrue(vm.state.value.selectedIds.isEmpty())
    }

    @Test
    fun `ClearSelection empties selectedIds`() {
        val o = outfit(id = "1")
        repository.outfits.add(o)
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))
        vm.onIntent(OutfitIntent.ClearSelection)

        assertTrue(vm.state.value.selectedIds.isEmpty())
    }

    // endregion

    // region DeleteSelected

    @Test
    fun `DeleteSelected removes outfits and sends OutfitsDeleted`() = runTest {
        val o1 = outfit(id = "1")
        val o2 = outfit(id = "2")
        repository.outfits.addAll(listOf(o1, o2))
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))

        vm.effects.test {
            vm.onIntent(OutfitIntent.DeleteSelected)

            val effect = awaitItem()
            assertIs<OutfitEffect.OutfitsDeleted>(effect)
        }
        assertTrue(vm.state.value.selectedIds.isEmpty())
        assertFalse(vm.state.value.isDeleting)
        assertEquals(listOf("1"), repository.deletedIds)
    }

    @Test
    fun `DeleteSelected partial failure sends ShowError`() = runTest {
        val o1 = outfit(id = "1")
        val o2 = outfit(id = "2")
        repository.outfits.addAll(listOf(o1, o2))
        val vm = createViewModel()

        vm.onIntent(OutfitIntent.ToggleSelection("1"))
        vm.onIntent(OutfitIntent.ToggleSelection("2"))
        repository.deleteOutfitError = RuntimeException("Failed")

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
