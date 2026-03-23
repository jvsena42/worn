package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.Outfit
import com.github.worn.domain.repository.OutfitRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OutfitIntent {
    data object LoadOutfits : OutfitIntent
    data class ToggleSelection(val outfitId: String) : OutfitIntent
    data object ClearSelection : OutfitIntent
    data object DeleteSelected : OutfitIntent
}

data class OutfitState(
    val outfits: List<Outfit> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val error: String? = null,
)

sealed interface OutfitEffect {
    data class ShowError(val message: String) : OutfitEffect
    data object OutfitsDeleted : OutfitEffect
}

class OutfitViewModel(
    private val repository: OutfitRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OutfitState())
    val state: StateFlow<OutfitState> = _state.asStateFlow()

    private val _effects = Channel<OutfitEffect>(Channel.BUFFERED)
    val effects: Flow<OutfitEffect> = _effects.receiveAsFlow()

    init {
        onIntent(OutfitIntent.LoadOutfits)
    }

    fun onIntent(intent: OutfitIntent) {
        when (intent) {
            is OutfitIntent.LoadOutfits -> loadOutfits()
            is OutfitIntent.ToggleSelection -> toggleSelection(intent.outfitId)
            is OutfitIntent.ClearSelection -> clearSelection()
            is OutfitIntent.DeleteSelected -> deleteSelected()
        }
    }

    private fun loadOutfits() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.getAll()
                .onSuccess { outfits ->
                    _state.update { it.copy(outfits = outfits, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                    _effects.send(OutfitEffect.ShowError(error.message ?: "Unknown error"))
                }
        }
    }

    private fun toggleSelection(outfitId: String) {
        _state.update { state ->
            val updated = if (outfitId in state.selectedIds) {
                state.selectedIds - outfitId
            } else {
                state.selectedIds + outfitId
            }
            state.copy(selectedIds = updated)
        }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedIds = emptySet()) }
    }

    private fun deleteSelected() {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    isDeleting = true,
                    outfits = state.outfits.filterNot { it.id in ids },
                    selectedIds = emptySet(),
                )
            }
            var failed = false
            for (id in ids) {
                repository.deleteOutfit(id).onFailure { failed = true }
            }
            _state.update { it.copy(isDeleting = false) }
            if (failed) {
                _effects.send(OutfitEffect.ShowError("Some outfits could not be deleted"))
            } else {
                _effects.send(OutfitEffect.OutfitsDeleted)
            }
            loadOutfits()
        }
    }
}
