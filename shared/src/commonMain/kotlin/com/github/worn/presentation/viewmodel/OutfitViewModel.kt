package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Outfit
import com.github.worn.domain.repository.OutfitRepository
import com.github.worn.domain.repository.WardrobeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OutfitIntent {
    data class FilterItemsByCategory(val category: Category?) : OutfitIntent
    data class ToggleItemSelection(val itemId: String) : OutfitIntent
    data class ToggleSelection(val outfitId: String) : OutfitIntent
    data object ClearSelection : OutfitIntent
    data object DeleteSelected : OutfitIntent
    data class CreateOutfit(val name: String) : OutfitIntent
    data class DeleteOutfit(val outfitId: String) : OutfitIntent
    data class UpdateOutfit(val outfit: Outfit) : OutfitIntent
}

data class OutfitState(
    val outfits: List<Outfit> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val error: String? = null,
    val itemCategories: Map<String, Category> = emptyMap(),
    val allClothingItems: List<ClothingItem> = emptyList(),
    val clothingItems: List<ClothingItem> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    val activeItemCategory: Category? = null,
    val isSaving: Boolean = false,
    val isLoadingItems: Boolean = false,
)

sealed interface OutfitEffect {
    data class ShowError(val message: String) : OutfitEffect
    data object OutfitsDeleted : OutfitEffect
    data object OutfitCreated : OutfitEffect
    data object OutfitDeleted : OutfitEffect
    data object OutfitUpdated : OutfitEffect
}

/** One wardrobe emission plus the lookup derived from it, so both share the same lifetime. */
private data class WardrobeSnapshot(
    val items: List<ClothingItem>,
    val categoriesById: Map<String, Category>,
)

@Suppress("TooManyFunctions")
class OutfitViewModel(
    private val repository: OutfitRepository,
    private val wardrobeRepository: WardrobeRepository,
) : ViewModel() {

    /** The parts of [OutfitState] not derived from the database. */
    private val _uiState = MutableStateFlow(OutfitState(isLoading = true))

    private val _effects = Channel<OutfitEffect>(Channel.BUFFERED)
    val effects: Flow<OutfitEffect> = _effects.receiveAsFlow()

    /**
     * Derived here rather than in the `combine` below on purpose: `map` only runs when the
     * wardrobe changes, so the Map instance survives unrelated state changes. Compose compares
     * unstable parameters like `Map` by instance, so rebuilding it would stop cards skipping.
     */
    private val wardrobeSnapshot: Flow<WardrobeSnapshot> =
        wardrobeRepository.observeAll()
            .catch { emit(emptyList()) }
            .map { items -> WardrobeSnapshot(items, items.associate { it.id to it.category }) }

    /**
     * Both tables are observed once and joined here, so the outfit list, the item picker and the
     * category dots share one source of truth instead of three refreshed copies.
     */
    val state: StateFlow<OutfitState> = combine(
        repository.observeAll().catch { error ->
            _uiState.update { it.copy(error = error.message) }
            _effects.send(OutfitEffect.ShowError(error.message ?: "Unknown error"))
            emit(emptyList())
        },
        wardrobeSnapshot,
        _uiState,
    ) { outfits, wardrobe, ui ->
        ui.copy(
            outfits = outfits,
            allClothingItems = wardrobe.items,
            itemCategories = wardrobe.categoriesById,
            clothingItems = ui.activeItemCategory
                ?.let { cat -> wardrobe.items.filter { it.category == cat } }
                ?: wardrobe.items,
            isLoading = false,
            isLoadingItems = false,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, OutfitState(isLoading = true))

    fun onIntent(intent: OutfitIntent) {
        when (intent) {
            is OutfitIntent.FilterItemsByCategory -> filterItemsByCategory(intent.category)
            is OutfitIntent.ToggleItemSelection -> toggleItemSelection(intent.itemId)
            is OutfitIntent.ToggleSelection -> toggleSelection(intent.outfitId)
            is OutfitIntent.ClearSelection -> clearSelection()
            is OutfitIntent.DeleteSelected -> deleteSelected()
            is OutfitIntent.CreateOutfit -> createOutfit(intent.name)
            is OutfitIntent.DeleteOutfit -> deleteOutfit(intent.outfitId)
            is OutfitIntent.UpdateOutfit -> updateOutfit(intent.outfit)
        }
    }

    private fun filterItemsByCategory(category: Category?) {
        _uiState.update { it.copy(activeItemCategory = category) }
    }

    private fun toggleItemSelection(itemId: String) {
        _uiState.update { state ->
            val updated = if (itemId in state.selectedItemIds) {
                state.selectedItemIds - itemId
            } else {
                state.selectedItemIds + itemId
            }
            state.copy(selectedItemIds = updated)
        }
    }

    private fun toggleSelection(outfitId: String) {
        _uiState.update { state ->
            val updated = if (outfitId in state.selectedIds) {
                state.selectedIds - outfitId
            } else {
                state.selectedIds + outfitId
            }
            state.copy(selectedIds = updated)
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    private fun createOutfit(name: String) {
        val itemIds = state.value.selectedItemIds.toList()
        if (name.isBlank() || itemIds.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.createOutfit(name = name, itemIds = itemIds)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, selectedItemIds = emptySet(), activeItemCategory = null)
                    }
                    _effects.send(OutfitEffect.OutfitCreated)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.send(OutfitEffect.ShowError(error.message ?: "Failed to create outfit"))
                }
        }
    }

    private fun deleteSelected() {
        val ids = state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, selectedIds = emptySet()) }
            var failed = false
            for (id in ids) {
                repository.deleteOutfit(id).onFailure { failed = true }
            }
            _uiState.update { it.copy(isDeleting = false) }
            if (failed) {
                _effects.send(OutfitEffect.ShowError("Some outfits could not be deleted"))
            } else {
                _effects.send(OutfitEffect.OutfitsDeleted)
            }
        }
    }

    private fun deleteOutfit(outfitId: String) {
        viewModelScope.launch {
            repository.deleteOutfit(outfitId)
                .onSuccess { _effects.send(OutfitEffect.OutfitDeleted) }
                .onFailure { _effects.send(OutfitEffect.ShowError(it.message ?: "Failed to delete")) }
        }
    }

    private fun updateOutfit(outfit: Outfit) {
        viewModelScope.launch {
            repository.updateOutfit(outfit)
                .onSuccess { _effects.send(OutfitEffect.OutfitUpdated) }
                .onFailure {
                    _effects.send(OutfitEffect.ShowError(it.message ?: "Failed to update"))
                }
        }
    }
}
