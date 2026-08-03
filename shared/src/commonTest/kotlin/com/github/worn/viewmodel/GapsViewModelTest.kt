package com.github.worn.viewmodel

import app.cash.turbine.test
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.capsuleWardrobeSuggestions
import com.github.worn.fake.FakeSettingsRepository
import com.github.worn.fake.FakeWardrobeRepository
import com.github.worn.fake.clothingItem
import com.github.worn.presentation.viewmodel.GapsEffect
import com.github.worn.presentation.viewmodel.GapsIntent
import com.github.worn.presentation.viewmodel.GapsViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GapsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeWardrobeRepository
    private lateinit var settingsRepository: FakeSettingsRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWardrobeRepository()
        settingsRepository = FakeSettingsRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): GapsViewModel =
        GapsViewModel(repository, settingsRepository)

    private fun aiRecommendation(
        name: String = "Navy overshirt",
        subcategory: Subcategory? = Subcategory.TRUCKER,
    ) = GapRecommendation(
        itemName = name,
        category = "LAYERING",
        pairingCount = 7,
        subcategory = subcategory,
        mappedCategory = Category.OUTERWEAR,
    )

    // region fallback mode

    @Test
    fun `init without AI shows the full capsule list and stops loading`() {
        val vm = createViewModel()

        assertEquals(capsuleWardrobeSuggestions, vm.state.value.recommendations)
        assertFalse(vm.state.value.isLoading)
        assertFalse(vm.state.value.isAiMode)
    }

    @Test
    fun `subcategories already owned are excluded from the capsule list`() {
        repository.addItems(clothingItem(subcategory = Subcategory.POLO))

        val vm = createViewModel()

        assertTrue(vm.state.value.recommendations.none { it.subcategory == Subcategory.POLO })
    }

    @Test
    fun `adding an item to the wardrobe removes its suggestion without any intent`() = runTest {
        val vm = createViewModel()

        vm.state.test {
            assertTrue(awaitItem().recommendations.any { it.subcategory == Subcategory.POLO })

            repository.addItems(clothingItem(id = "polo", subcategory = Subcategory.POLO))

            assertTrue(awaitItem().recommendations.none { it.subcategory == Subcategory.POLO })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting an item brings its suggestion back`() = runTest {
        repository.addItems(clothingItem(id = "polo", subcategory = Subcategory.POLO))
        val vm = createViewModel()

        vm.state.test {
            assertTrue(awaitItem().recommendations.none { it.subcategory == Subcategory.POLO })

            repository.deleteItem("polo")

            assertTrue(awaitItem().recommendations.any { it.subcategory == Subcategory.POLO })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a wardrobe stream failure falls back to the unfiltered capsule list`() = runTest {
        repository.observeAllError = IllegalStateException("db gone")

        val vm = createViewModel()

        assertEquals(capsuleWardrobeSuggestions, vm.state.value.recommendations)
        assertNull(vm.state.value.error)
        vm.effects.test {
            assertEquals("db gone", assertIs<GapsEffect.ShowError>(awaitItem()).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LoadGaps in fallback mode does not call the AI`() {
        val vm = createViewModel()

        vm.onIntent(GapsIntent.LoadGaps)

        assertEquals(0, repository.gapRecommendationCalls)
        assertFalse(vm.state.value.isLoading)
    }

    // endregion

    // region AI mode

    @Test
    fun `init with a Claude key loads AI recommendations`() {
        settingsRepository.apiKey = "test-key"
        repository.gapRecommendations = listOf(aiRecommendation())

        val vm = createViewModel()

        assertTrue(vm.state.value.isAiMode)
        assertEquals(listOf("Navy overshirt"), vm.state.value.recommendations.map { it.itemName })
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `AI recommendations for an owned subcategory are filtered out`() {
        settingsRepository.apiKey = "test-key"
        repository.gapRecommendations = listOf(aiRecommendation())
        repository.addItems(clothingItem(subcategory = Subcategory.TRUCKER))

        val vm = createViewModel()

        assertTrue(vm.state.value.recommendations.isEmpty())
    }

    @Test
    fun `adding an item re-filters the AI list without a new AI call`() = runTest {
        settingsRepository.apiKey = "test-key"
        repository.gapRecommendations = listOf(aiRecommendation())
        val vm = createViewModel()

        vm.state.test {
            assertEquals(1, awaitItem().recommendations.size)

            repository.addItems(clothingItem(subcategory = Subcategory.TRUCKER))

            assertTrue(awaitItem().recommendations.isEmpty())
            assertEquals(1, repository.gapRecommendationCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an AI recommendation without a subcategory is always kept`() {
        settingsRepository.apiKey = "test-key"
        repository.gapRecommendations = listOf(aiRecommendation(subcategory = null))
        repository.addItems(clothingItem(subcategory = Subcategory.TRUCKER))

        val vm = createViewModel()

        assertEquals(1, vm.state.value.recommendations.size)
    }

    @Test
    fun `an AI failure clears the list and reports the error`() = runTest {
        settingsRepository.apiKey = "test-key"
        repository.gapRecommendationsError = IllegalStateException("api down")

        val vm = createViewModel()

        assertTrue(vm.state.value.recommendations.isEmpty())
        assertEquals("api down", vm.state.value.error)
        vm.effects.test {
            assertEquals("api down", assertIs<GapsEffect.ShowError>(awaitItem()).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an AI failure with a null message still sets a non-null error`() {
        settingsRepository.apiKey = "test-key"
        repository.gapRecommendationsError = IllegalStateException()

        val vm = createViewModel()

        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `LoadGaps retries the AI call`() {
        settingsRepository.apiKey = "test-key"
        val vm = createViewModel()
        assertEquals(1, repository.gapRecommendationCalls)

        vm.onIntent(GapsIntent.LoadGaps)

        assertEquals(2, repository.gapRecommendationCalls)
    }

    @Test
    fun `enabling AI after construction switches from the capsule list to AI recommendations`() {
        repository.gapRecommendations = listOf(aiRecommendation())
        val vm = createViewModel()
        assertEquals(capsuleWardrobeSuggestions, vm.state.value.recommendations)

        settingsRepository.apiKey = "test-key"

        assertTrue(vm.state.value.isAiMode)
        assertEquals(listOf("Navy overshirt"), vm.state.value.recommendations.map { it.itemName })
    }

    // endregion

}
