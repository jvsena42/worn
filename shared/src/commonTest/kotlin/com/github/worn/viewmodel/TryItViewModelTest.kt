package com.github.worn.viewmodel

import com.github.worn.domain.model.GarmentCategory
import com.github.worn.fake.FakeSecretStore
import com.github.worn.fake.FakeSettingsRepository
import com.github.worn.fake.FakeTryOnRepository
import com.github.worn.fake.FakeWardrobeRepository
import com.github.worn.presentation.viewmodel.TryItIntent
import com.github.worn.presentation.viewmodel.TryItViewModel
import com.github.worn.util.secret.SecretStore
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
    private lateinit var secretStore: FakeSecretStore

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        wardrobe = FakeWardrobeRepository()
        tryOn = FakeTryOnRepository()
        settings = FakeSettingsRepository()
        secretStore = FakeSecretStore()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TryItViewModel(wardrobe, tryOn, settings, secretStore)

    @Test
    fun `init reports no capabilities when neither key is set`() {
        val vm = createViewModel()
        assertFalse(vm.state.value.hasApiKey)
        assertFalse(vm.state.value.hasYouCamKey)
    }

    @Test
    fun `init reports YouCam capability when both credentials are set`() {
        secretStore.saveSecret(SecretStore.YOUCAM_CLIENT_ID, "id")
        secretStore.saveSecret(SecretStore.YOUCAM_CLIENT_SECRET, "secret")
        val vm = createViewModel()
        assertTrue(vm.state.value.hasYouCamKey)
        assertFalse(vm.state.value.hasApiKey)
    }

    @Test
    fun `init does not report YouCam capability when only one credential is set`() {
        secretStore.saveSecret(SecretStore.YOUCAM_CLIENT_ID, "id")
        val vm = createViewModel()
        assertFalse(vm.state.value.hasYouCamKey)
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
}
