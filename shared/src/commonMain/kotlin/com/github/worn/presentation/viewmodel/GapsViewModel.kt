package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.capsuleWardrobeSuggestions
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

sealed interface GapsIntent {
    data object LoadGaps : GapsIntent
}

data class GapsState(
    val recommendations: List<GapRecommendation> = emptyList(),
    val isLoading: Boolean = false,
    val hasApiKey: Boolean = false,
    val isAiMode: Boolean = false,
    val error: String? = null,
)

sealed interface GapsEffect {
    data class ShowError(val message: String) : GapsEffect
}

class GapsViewModel(
    private val wardrobeRepository: WardrobeRepository,
    private val secretStore: SecretStore,
) : ViewModel() {

    private val _state = MutableStateFlow(GapsState())
    val state: StateFlow<GapsState> = _state.asStateFlow()

    private val _effects = Channel<GapsEffect>(Channel.BUFFERED)
    val effects: Flow<GapsEffect> = _effects.receiveAsFlow()

    init {
        val hasKey = secretStore.getApiKey() != null
        _state.update { it.copy(hasApiKey = hasKey, isAiMode = hasKey) }
        onIntent(GapsIntent.LoadGaps)
    }

    fun onIntent(intent: GapsIntent) {
        when (intent) {
            is GapsIntent.LoadGaps -> loadGaps()
        }
    }

    private fun loadGaps() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            if (_state.value.isAiMode) {
                loadAiRecommendations()
            } else {
                loadFallbackSuggestions()
            }
        }
    }

    private suspend fun loadAiRecommendations() {
        wardrobeRepository.getGapRecommendations()
            .onSuccess { recommendations ->
                _state.update { it.copy(recommendations = recommendations, isLoading = false) }
            }
            .onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
                _effects.send(GapsEffect.ShowError(error.message ?: "Failed to load recommendations"))
            }
    }

    private suspend fun loadFallbackSuggestions() {
        wardrobeRepository.getAll()
            .onSuccess { items ->
                val ownedSubcategories = items.mapNotNull { it.subcategory }.toSet()
                val filtered = capsuleWardrobeSuggestions.filter {
                    it.subcategory !in ownedSubcategories
                }
                _state.update { it.copy(recommendations = filtered, isLoading = false) }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        recommendations = capsuleWardrobeSuggestions,
                        isLoading = false,
                    )
                }
                _effects.send(GapsEffect.ShowError(error.message ?: "Failed to load wardrobe"))
            }
    }
}
