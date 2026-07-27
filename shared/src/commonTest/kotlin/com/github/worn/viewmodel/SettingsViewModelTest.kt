package com.github.worn.viewmodel

import com.github.worn.domain.model.OnDeviceAiAvailability
import com.github.worn.domain.model.OnDeviceAiUnavailableReason
import com.github.worn.fake.FakeSettingsRepository
import com.github.worn.fake.FakeTryOnRepository
import com.github.worn.presentation.viewmodel.SettingsIntent
import com.github.worn.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settings: FakeSettingsRepository
    private lateinit var tryOn: FakeTryOnRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settings = FakeSettingsRepository()
        tryOn = FakeTryOnRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(settings, tryOn)

    // region on-device AI

    @Test
    fun `init reports on-device AI as unavailable by default`() {
        val vm = createViewModel()

        assertFalse(vm.state.value.onDeviceAiEnabled)
        assertEquals(
            OnDeviceAiAvailability.Unavailable(OnDeviceAiUnavailableReason.UNSUPPORTED_DEVICE),
            vm.state.value.onDeviceAiAvailability,
        )
    }

    @Test
    fun `init surfaces the availability reported by the repository`() {
        settings.onDeviceAiAvailability = OnDeviceAiAvailability.Available

        val vm = createViewModel()

        assertEquals(OnDeviceAiAvailability.Available, vm.state.value.onDeviceAiAvailability)
    }

    @Test
    fun `init keeps the preference when the device still supports it`() {
        settings.onDeviceAiAvailability = OnDeviceAiAvailability.Available
        settings.onDeviceAiEnabled.value = true

        val vm = createViewModel()

        assertTrue(vm.state.value.onDeviceAiEnabled)
        assertTrue(settings.onDeviceAiEnabled.value)
    }

    /** Apple Intelligence can be switched off, or the model evicted, after the user opted in. */
    @Test
    fun `init clears the preference when the device lost support`() {
        settings.onDeviceAiEnabled.value = true
        settings.onDeviceAiAvailability =
            OnDeviceAiAvailability.Unavailable(OnDeviceAiUnavailableReason.DISABLED_BY_USER)

        val vm = createViewModel()

        assertFalse(vm.state.value.onDeviceAiEnabled)
        assertFalse(settings.onDeviceAiEnabled.value, "The stored preference should be cleared too")
    }

    @Test
    fun `init treats a downloadable model as still opted in`() {
        settings.onDeviceAiEnabled.value = true
        settings.onDeviceAiAvailability = OnDeviceAiAvailability.Downloadable

        val vm = createViewModel()

        assertTrue(vm.state.value.onDeviceAiEnabled)
    }

    @Test
    fun `SetOnDeviceAi persists the preference and updates state`() {
        settings.onDeviceAiAvailability = OnDeviceAiAvailability.Available
        val vm = createViewModel()

        vm.onIntent(SettingsIntent.SetOnDeviceAi(enabled = true))

        assertTrue(vm.state.value.onDeviceAiEnabled)
        assertTrue(settings.onDeviceAiEnabled.value)
    }

    @Test
    fun `SetOnDeviceAi turns the preference back off`() {
        settings.onDeviceAiAvailability = OnDeviceAiAvailability.Available
        settings.onDeviceAiEnabled.value = true
        val vm = createViewModel()

        vm.onIntent(SettingsIntent.SetOnDeviceAi(enabled = false))

        assertFalse(vm.state.value.onDeviceAiEnabled)
        assertFalse(settings.onDeviceAiEnabled.value)
    }

    // endregion

    // region credentials

    @Test
    fun `init reports an existing Claude key`() {
        settings.apiKey = "sk-ant-test"

        val vm = createViewModel()

        assertTrue(vm.state.value.hasApiKey)
    }

    @Test
    fun `SaveApiKey stores the key and flips hasApiKey`() {
        val vm = createViewModel()

        vm.onIntent(SettingsIntent.SaveApiKey("sk-ant-test"))

        assertTrue(vm.state.value.hasApiKey)
        assertEquals("sk-ant-test", settings.apiKey)
    }

    @Test
    fun `ClearApiKey removes the key`() {
        settings.apiKey = "sk-ant-test"
        val vm = createViewModel()

        vm.onIntent(SettingsIntent.ClearApiKey)

        assertFalse(vm.state.value.hasApiKey)
        assertEquals(null, settings.apiKey)
    }

    // endregion
}
