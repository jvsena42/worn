package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.capsuleWardrobeSuggestions
import com.github.worn.domain.model.excludingOwned
import com.github.worn.domain.repository.SettingsRepository
import com.github.worn.domain.repository.WardrobeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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

/**
 * The half of [GapsState] that is not derived from the wardrobe stream.
 *
 * [aiRecommendations] holds the list exactly as the model returned it; the ownership filter runs
 * downstream in [GapsViewModel.state], so a wardrobe write re-filters it without a new AI call.
 */
private data class GapsUiState(
    val aiRecommendations: List<GapRecommendation> = emptyList(),
    val isLoading: Boolean = true,
    val isAiAvailable: Boolean = false,
    val isAiMode: Boolean = false,
    val error: String? = null,
)

class GapsViewModel(
    private val wardrobeRepository: WardrobeRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GapsUiState())

    private val _effects = Channel<GapsEffect>(Channel.BUFFERED)
    val effects: Flow<GapsEffect> = _effects.receiveAsFlow()

    /**
     * Suggestions are derived, never stored: the wardrobe stream re-runs the ownership filter on
     * every write, so an item added on any tab makes its suggestion disappear here immediately.
     * Only the AI list is fetched, and only on init, on an availability change, or on an explicit
     * retry — a wardrobe emission must never cost a request.
     *
     * [SharingStarted.Eagerly] keeps the last value cached, so returning to the tab renders
     * immediately rather than behind a spinner.
     */
    val state: StateFlow<GapsState> = combine(
        wardrobeRepository.observeAll().catch { error ->
            _effects.send(GapsEffect.ShowError(error.message ?: UNKNOWN_ERROR))
            emit(emptyList())
        },
        _uiState,
    ) { items, ui ->
        val source = if (ui.isAiMode) ui.aiRecommendations else capsuleWardrobeSuggestions
        GapsState(
            recommendations = source.excludingOwned(items),
            isLoading = ui.isLoading,
            isAiAvailable = ui.isAiAvailable,
            isAiMode = ui.isAiMode,
            error = ui.error,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GapsState(isLoading = true))

    init {
        observeAiAvailability()
    }

    /**
     * Collected, not read once: a key added on Settings has to switch this screen over to AI
     * recommendations without a restart. The fallback needs no fetch at all — it is a constant, and
     * the filter above already tracks the wardrobe.
     */
    private fun observeAiAvailability() {
        viewModelScope.launch {
            settingsRepository.isAiAvailableFlow().collect { hasAi ->
                _uiState.update {
                    it.copy(isAiAvailable = hasAi, isAiMode = hasAi, isLoading = hasAi, error = null)
                }
                if (hasAi) loadAiRecommendations()
            }
        }
    }

    fun onIntent(intent: GapsIntent) {
        when (intent) {
            is GapsIntent.LoadGaps -> retry()
        }
    }

    /** Only the AI call can fail; the capsule list is a constant filtered against a live wardrobe. */
    private fun retry() {
        viewModelScope.launch {
            if (_uiState.value.isAiMode) {
                loadAiRecommendations()
            } else {
                _uiState.update { it.copy(isLoading = false, error = null) }
            }
        }
    }

    private suspend fun loadAiRecommendations() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        wardrobeRepository.getGapRecommendations()
            .onSuccess { recommendations ->
                _uiState.update { it.copy(aiRecommendations = recommendations, isLoading = false) }
            }
            .onFailure { error ->
                // Defaulted rather than passed through: a null message would leave the error state
                // clear and render the "wardrobe complete" screen on a failure.
                val message = error.message ?: RECOMMENDATIONS_ERROR
                _uiState.update {
                    it.copy(aiRecommendations = emptyList(), isLoading = false, error = message)
                }
                _effects.send(GapsEffect.ShowError(message))
            }
    }

    private companion object {
        const val UNKNOWN_ERROR = "Unknown error"
        const val RECOMMENDATIONS_ERROR = "Failed to load recommendations"
    }
}
