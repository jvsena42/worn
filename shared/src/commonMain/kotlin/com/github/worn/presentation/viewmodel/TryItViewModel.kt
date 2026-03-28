package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.TryItResult
import com.github.worn.domain.repository.WardrobeRepository
import com.github.worn.util.secret.SecretStore
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
    data object Reset : TryItIntent
}

data class TryItState(
    val isLoading: Boolean = false,
    val hasApiKey: Boolean = false,
    val result: TryItResult? = null,
    val error: String? = null,
)

sealed interface TryItEffect {
    data class ShowError(val message: String) : TryItEffect
}

class TryItViewModel(
    private val wardrobeRepository: WardrobeRepository,
    private val secretStore: SecretStore,
) : ViewModel() {

    private val _state = MutableStateFlow(TryItState())
    val state: StateFlow<TryItState> = _state.asStateFlow()

    private val _effects = Channel<TryItEffect>(Channel.BUFFERED)
    val effects: Flow<TryItEffect> = _effects.receiveAsFlow()

    init {
        _state.update { it.copy(hasApiKey = secretStore.getApiKey() != null) }
    }

    fun onIntent(intent: TryItIntent) {
        when (intent) {
            is TryItIntent.AnalyzePhoto -> analyzePhoto(intent.imageBytes)
            is TryItIntent.Reset -> reset()
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

    private fun reset() {
        _state.update { it.copy(result = null, error = null) }
    }
}
