package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.capsuleWardrobeSuggestions
import com.github.worn.domain.repository.SettingsRepository
import com.github.worn.domain.repository.WardrobeRepository
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
    /** A Claude key is configured, or on-device AI is enabled and available. */
    val isAiAvailable: Boolean = false,
    val isAiMode: Boolean = false,
    val error: String? = null,
)

sealed interface GapsEffect {
    data class ShowError(val message: String) : GapsEffect
}

class GapsViewModel(
    private val wardrobeRepository: WardrobeRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GapsState())
    val state: StateFlow<GapsState> = _state.asStateFlow()

    private val _effects = Channel<GapsEffect>(Channel.BUFFERED)
    val effects: Flow<GapsEffect> = _effects.receiveAsFlow()

    init {
        // Resolve the provider before branching, and off the main thread — see SettingsRepository.
        // Collected, not read once: adding a key on Settings has to switch this screen over to AI
        // recommendations without a restart, which means recomputing the gaps too.
        viewModelScope.launch {
            settingsRepository.isAiAvailableFlow().collect { hasAi ->
                _state.update { it.copy(isAiAvailable = hasAi, isAiMode = hasAi) }
                loadGaps()
            }
        }
    }

    fun onIntent(intent: GapsIntent) {
        when (intent) {
            is GapsIntent.LoadGaps -> viewModelScope.launch { loadGaps() }
        }
    }

    private suspend fun loadGaps() {
        _state.update { it.copy(isLoading = true, error = null) }
        if (_state.value.isAiMode) {
            loadAiRecommendations()
        } else {
            loadFallbackSuggestions()
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
