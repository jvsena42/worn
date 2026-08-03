package com.github.worn.viewmodel

import com.github.worn.domain.model.GarmentCategory
import com.github.worn.domain.model.TryItFeature
import com.github.worn.fake.FakeSettingsRepository
import com.github.worn.fake.FakeTryOnRepository
import com.github.worn.fake.FakeWardrobeRepository
import com.github.worn.presentation.viewmodel.TryItIntent
import com.github.worn.presentation.viewmodel.TryItViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TryItViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var wardrobe: FakeWardrobeRepository
    private lateinit var tryOn: FakeTryOnRepository
    private lateinit var settings: FakeSettingsRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        wardrobe = FakeWardrobeRepository()
        tryOn = FakeTryOnRepository()
        settings = FakeSettingsRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TryItViewModel(wardrobe, tryOn, settings)

    @Test
    fun `init reports no capabilities when neither key is set`() {
        val vm = createViewModel()
        assertFalse(vm.state.value.hasApiKey)
        assertFalse(vm.state.value.hasYouCamKey)
    }

    @Test
    fun `init reports YouCam capability when both credentials are set`() {
        settings.youCamClientId = "id"
        settings.youCamClientSecret = "secret"
        val vm = createViewModel()
        assertTrue(vm.state.value.hasYouCamKey)
        assertFalse(vm.state.value.hasApiKey)
    }

    @Test
    fun `init does not report YouCam capability when only one credential is set`() {
        settings.youCamClientId = "id"
        val vm = createViewModel()
        assertFalse(vm.state.value.hasYouCamKey)
    }

    @Test
    fun `YouCam credentials saved after construction unlock the screen without a restart`() {
        val vm = createViewModel()
        assertFalse(vm.state.value.hasYouCamKey)

        settings.youCamClientId = "id"
        settings.youCamClientSecret = "secret"

        assertTrue(vm.state.value.hasYouCamKey)
    }

    @Test
    fun `clearing YouCam credentials after construction re-locks the screen`() {
        settings.youCamClientId = "id"
        settings.youCamClientSecret = "secret"
        val vm = createViewModel()

        settings.youCamClientSecret = null

        assertFalse(vm.state.value.hasYouCamKey)
    }

    @Test
    fun `a Claude key saved after construction unlocks analysis without a restart`() {
        val vm = createViewModel()
        assertFalse(vm.state.value.hasApiKey)

        settings.apiKey = "test-key"

        assertTrue(vm.state.value.hasApiKey)
    }

    @Test
    fun `init loads the saved person photo`() {
        settings.modelPhoto.value = byteArrayOf(1, 2)
        val vm = createViewModel()
        assertContentEquals(byteArrayOf(1, 2), vm.state.value.personImage)
    }

    @Test
    fun `SetPersonPhoto updates state and persists the photo`() = runTest {
        val vm = createViewModel()
        val photo = byteArrayOf(5, 5)

        vm.onIntent(TryItIntent.SetPersonPhoto(photo))

        assertContentEquals(photo, vm.state.value.personImage)
        assertContentEquals(photo, settings.modelPhoto.value)
    }

    @Test
    fun `SelectCategory updates the selected category`() {
        val vm = createViewModel()
        vm.onIntent(TryItIntent.SelectCategory(GarmentCategory.BOTTOM))
        assertEquals(GarmentCategory.BOTTOM, vm.state.value.selectedCategory)
    }

    @Test
    fun `GenerateTryOn sets the rendered image on success`() = runTest {
        val rendered = byteArrayOf(7, 7, 7)
        tryOn.result = Result.success(rendered)
        val vm = createViewModel()
        vm.onIntent(TryItIntent.SelectCategory(GarmentCategory.TOP))

        vm.onIntent(TryItIntent.GenerateTryOn(byteArrayOf(1)))

        assertContentEquals(rendered, vm.state.value.tryOnImage)
        assertFalse(vm.state.value.tryOnLoading)
        assertEquals(1, tryOn.calls.size)
    }

    @Test
    fun `GenerateTryOn is a no-op when no category is selected`() = runTest {
        val vm = createViewModel()
        vm.onIntent(TryItIntent.GenerateTryOn(byteArrayOf(1)))
        assertTrue(tryOn.calls.isEmpty())
        assertNull(vm.state.value.tryOnImage)
    }

    // region Shared-photo routing

    private fun connectClaude() {
        settings.apiKey = "sk-ant-test"
    }

    private fun connectYouCam() {
        settings.youCamClientId = "id"
        settings.youCamClientSecret = "secret"
    }

    @Test
    fun `ReceiveSharedPhoto focuses analysis when only Claude is connected`() = runTest {
        connectClaude()
        val vm = createViewModel()

        vm.onIntent(TryItIntent.ReceiveSharedPhoto)

        assertEquals(TryItFeature.ANALYSIS, vm.state.value.focusedFeature)
        assertFalse(vm.state.value.featureChoiceRequired)
    }

    @Test
    fun `ReceiveSharedPhoto focuses try-on when only YouCam is connected`() = runTest {
        connectYouCam()
        val vm = createViewModel()

        vm.onIntent(TryItIntent.ReceiveSharedPhoto)

        assertEquals(TryItFeature.VIRTUAL_TRY_ON, vm.state.value.focusedFeature)
        assertFalse(vm.state.value.featureChoiceRequired)
    }

    @Test
    fun `ReceiveSharedPhoto asks which feature when both are connected`() = runTest {
        connectClaude()
        connectYouCam()
        val vm = createViewModel()

        vm.onIntent(TryItIntent.ReceiveSharedPhoto)

        assertTrue(vm.state.value.featureChoiceRequired)
        assertNull(vm.state.value.focusedFeature)
    }

    @Test
    fun `ReceiveSharedPhoto neither asks nor focuses when no credential is connected`() = runTest {
        val vm = createViewModel()

        vm.onIntent(TryItIntent.ReceiveSharedPhoto)

        assertFalse(vm.state.value.featureChoiceRequired)
        assertNull(vm.state.value.focusedFeature)
    }

    @Test
    fun `ReceiveSharedPhoto reads credentials connected after the ViewModel was built`() = runTest {
        val vm = createViewModel()
        assertFalse(vm.state.value.hasApiKey)

        connectClaude()
        vm.onIntent(TryItIntent.ReceiveSharedPhoto)

        assertTrue(vm.state.value.hasApiKey)
        assertEquals(TryItFeature.ANALYSIS, vm.state.value.focusedFeature)
    }

    @Test
    fun `ChooseFeature focuses the picked feature and dismisses the chooser`() = runTest {
        connectClaude()
        connectYouCam()
        val vm = createViewModel()
        vm.onIntent(TryItIntent.ReceiveSharedPhoto)

        vm.onIntent(TryItIntent.ChooseFeature(TryItFeature.VIRTUAL_TRY_ON))

        assertEquals(TryItFeature.VIRTUAL_TRY_ON, vm.state.value.focusedFeature)
        assertFalse(vm.state.value.featureChoiceRequired)
    }

    @Test
    fun `ClearFeatureFocus clears both the focus and the chooser`() = runTest {
        connectClaude()
        connectYouCam()
        val vm = createViewModel()
        vm.onIntent(TryItIntent.ReceiveSharedPhoto)

        vm.onIntent(TryItIntent.ClearFeatureFocus)

        assertNull(vm.state.value.focusedFeature)
        assertFalse(vm.state.value.featureChoiceRequired)
    }

    // endregion
}
