package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.GarmentCategory
import com.github.worn.domain.model.TryItResult
import com.github.worn.domain.repository.SettingsRepository
import com.github.worn.domain.repository.TryOnRepository
import com.github.worn.domain.repository.WardrobeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        viewModelScope.launch {
            val hasApiKey = settingsRepository.hasApiKey().getOrDefault(false)
            val hasYouCamKey = settingsRepository.hasYouCamCredentials().getOrDefault(false)
            _state.update { it.copy(hasApiKey = hasApiKey, hasYouCamKey = hasYouCamKey) }
        }
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
