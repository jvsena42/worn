package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.GarmentCategory
import com.github.worn.domain.model.TryItFeature
import com.github.worn.domain.model.TryItResult
import com.github.worn.domain.repository.SettingsRepository
import com.github.worn.domain.repository.TryOnRepository
import com.github.worn.domain.repository.WardrobeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TryItIntent {
    data class AnalyzePhoto(val imageBytes: ByteArray) : TryItIntent
    data class SelectCategory(val category: GarmentCategory) : TryItIntent
    data class SetPersonPhoto(val imageBytes: ByteArray) : TryItIntent
    data class GenerateTryOn(val garmentBytes: ByteArray) : TryItIntent
    data object ResetTryOn : TryItIntent
    data object Reset : TryItIntent

    /** A photo arrived from outside the app (system share). The bytes stay in the UI layer. */
    data object ReceiveSharedPhoto : TryItIntent
    data class ChooseFeature(val feature: TryItFeature) : TryItIntent
    data object ClearFeatureFocus : TryItIntent
}

data class TryItState(
    val isLoading: Boolean = false,
    val hasApiKey: Boolean = false,
    val result: TryItResult? = null,
    val error: String? = null,
    // Virtual try-on (YouCam)
    val hasYouCamKey: Boolean = false,
    val personImage: ByteArray? = null,
    val selectedCategory: GarmentCategory? = null,
    val tryOnLoading: Boolean = false,
    val tryOnImage: ByteArray? = null,
    val tryOnError: String? = null,
    // Routing for a photo received from the system share sheet
    val featureChoiceRequired: Boolean = false,
    val focusedFeature: TryItFeature? = null,
)

sealed interface TryItEffect {
    data class ShowError(val message: String) : TryItEffect
}

class TryItViewModel(
    private val wardrobeRepository: WardrobeRepository,
    private val tryOnRepository: TryOnRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TryItState())
    val state: StateFlow<TryItState> = _state.asStateFlow()

    private val _effects = Channel<TryItEffect>(Channel.BUFFERED)
    val effects: Flow<TryItEffect> = _effects.receiveAsFlow()

    init {
        observeCredentials()
        viewModelScope.launch {
            settingsRepository.getModelPhoto().onSuccess { bytes ->
                _state.update { it.copy(personImage = bytes) }
            }
        }
    }

    fun onIntent(intent: TryItIntent) {
        when (intent) {
            is TryItIntent.AnalyzePhoto -> analyzePhoto(intent.imageBytes)
            is TryItIntent.SelectCategory ->
                _state.update { it.copy(selectedCategory = intent.category, tryOnError = null) }
            is TryItIntent.SetPersonPhoto -> setPersonPhoto(intent.imageBytes)
            is TryItIntent.GenerateTryOn -> generateTryOn(intent.garmentBytes)
            is TryItIntent.ResetTryOn ->
                _state.update { it.copy(tryOnImage = null, tryOnError = null) }
            is TryItIntent.Reset -> reset()
            is TryItIntent.ReceiveSharedPhoto -> receiveSharedPhoto()
            is TryItIntent.ChooseFeature ->
                _state.update {
                    it.copy(focusedFeature = intent.feature, featureChoiceRequired = false)
                }
            is TryItIntent.ClearFeatureFocus ->
                _state.update { it.copy(focusedFeature = null, featureChoiceRequired = false) }
        }
    }

    /**
     * Keys are entered on the Settings screen while this ViewModel stays alive — both platforms
     * keep every tab's screen in memory — so the gate has to follow the credentials rather than
     * sample them once at construction.
     */
    private fun observeCredentials() {
        viewModelScope.launch {
            combine(
                settingsRepository.hasApiKeyFlow(),
                settingsRepository.hasYouCamCredentialsFlow(),
                ::Pair,
            ).collect { (hasApiKey, hasYouCamKey) ->
                _state.update { it.copy(hasApiKey = hasApiKey, hasYouCamKey = hasYouCamKey) }
            }
        }
    }

    /**
     * Reads both credential flags into state and returns them, so callers that need to branch on
     * them do not race the [observeCredentials] collector's first emission.
     */
    private suspend fun loadCredentialState(): Pair<Boolean, Boolean> {
        val hasApiKey = settingsRepository.hasApiKey().getOrDefault(false)
        val hasYouCamKey = settingsRepository.hasYouCamCredentials().getOrDefault(false)
        _state.update { it.copy(hasApiKey = hasApiKey, hasYouCamKey = hasYouCamKey) }
        return hasApiKey to hasYouCamKey
    }

    /**
     * A share can arrive before [init] finishes loading credentials, so this re-reads them rather
     * than trusting the current state. With one credential the feature is unambiguous; with both
     * the user picks. With neither the screen already shows its locked state.
     */
    private fun receiveSharedPhoto() {
        viewModelScope.launch {
            val (hasApiKey, hasYouCamKey) = loadCredentialState()
            _state.update {
                when {
                    hasApiKey && hasYouCamKey ->
                        it.copy(featureChoiceRequired = true, focusedFeature = null)
                    hasApiKey ->
                        it.copy(featureChoiceRequired = false, focusedFeature = TryItFeature.ANALYSIS)
                    hasYouCamKey ->
                        it.copy(
                            featureChoiceRequired = false,
                            focusedFeature = TryItFeature.VIRTUAL_TRY_ON,
                        )
                    else -> it.copy(featureChoiceRequired = false, focusedFeature = null)
                }
            }
        }
    }

    private fun setPersonPhoto(imageBytes: ByteArray) {
        _state.update { it.copy(personImage = imageBytes, tryOnError = null) }
        viewModelScope.launch {
            settingsRepository.saveModelPhoto(imageBytes).onFailure { error ->
                _effects.send(TryItEffect.ShowError(error.message ?: "Failed to save photo"))
            }
        }
    }

    private fun analyzePhoto(imageBytes: ByteArray) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, result = null) }
            wardrobeRepository.analyzeProspectiveItem(imageBytes)
                .onSuccess { result ->
                    _state.update { it.copy(result = result, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                    _effects.send(
                        TryItEffect.ShowError(error.message ?: "Failed to analyze item"),
                    )
                }
        }
    }

    private fun generateTryOn(garmentBytes: ByteArray) {
        val category = _state.value.selectedCategory ?: return
        viewModelScope.launch {
            _state.update { it.copy(tryOnLoading = true, tryOnError = null, tryOnImage = null) }
            tryOnRepository.generateTryOn(garmentBytes, category)
                .onSuccess { image ->
                    _state.update { it.copy(tryOnImage = image, tryOnLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(tryOnLoading = false, tryOnError = error.message) }
                    _effects.send(
                        TryItEffect.ShowError(error.message ?: "Failed to generate try-on"),
                    )
                }
        }
    }

    private fun reset() {
        _state.update {
            it.copy(result = null, error = null, tryOnImage = null, tryOnError = null)
        }
    }
}
