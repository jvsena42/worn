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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OutfitIntent {
    data object LoadOutfits : OutfitIntent
    data object LoadClothingItems : OutfitIntent
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

class OutfitViewModel(
    private val repository: OutfitRepository,
    private val wardrobeRepository: WardrobeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OutfitState())
    val state: StateFlow<OutfitState> = _state.asStateFlow()

    private val _effects = Channel<OutfitEffect>(Channel.BUFFERED)
    val effects: Flow<OutfitEffect> = _effects.receiveAsFlow()

    init {
        onIntent(OutfitIntent.LoadOutfits)
        loadItemCategories()
    }

    private fun loadItemCategories() {
        viewModelScope.launch {
            wardrobeRepository.getAll().onSuccess { items ->
                val categories = items.associate { it.id to it.category }
                _state.update { it.copy(itemCategories = categories, allClothingItems = items) }
            }
        }
    }

    fun onIntent(intent: OutfitIntent) {
        when (intent) {
            is OutfitIntent.LoadOutfits -> loadOutfits()
            is OutfitIntent.LoadClothingItems -> loadClothingItems()
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

    private fun loadClothingItems() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingItems = true) }
            val result = when (val cat = _state.value.activeItemCategory) {
                null -> wardrobeRepository.getAll()
                else -> wardrobeRepository.getByCategory(cat)
            }
            result.onSuccess { items ->
                _state.update { it.copy(clothingItems = items, isLoadingItems = false) }
            }.onFailure { error ->
                _state.update { it.copy(isLoadingItems = false) }
                _effects.send(OutfitEffect.ShowError(error.message ?: "Failed to load items"))
            }
        }
    }

    private fun filterItemsByCategory(category: Category?) {
        _state.update { it.copy(activeItemCategory = category) }
        loadClothingItems()
    }

    private fun toggleItemSelection(itemId: String) {
        _state.update { state ->
            val updated = if (itemId in state.selectedItemIds) {
                state.selectedItemIds - itemId
            } else {
                state.selectedItemIds + itemId
            }
            state.copy(selectedItemIds = updated)
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

    private fun createOutfit(name: String) {
        val itemIds = _state.value.selectedItemIds.toList()
        if (name.isBlank() || itemIds.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            repository.createOutfit(name = name, itemIds = itemIds)
                .onSuccess {
                    _state.update {
                        it.copy(isSaving = false, selectedItemIds = emptySet(), activeItemCategory = null)
                    }
                    _effects.send(OutfitEffect.OutfitCreated)
                    loadOutfits()
                }
                .onFailure { error ->
                    _state.update { it.copy(isSaving = false) }
                    _effects.send(OutfitEffect.ShowError(error.message ?: "Failed to create outfit"))
                }
        }
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

    private fun deleteOutfit(outfitId: String) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(outfits = state.outfits.filterNot { it.id == outfitId })
            }
            repository.deleteOutfit(outfitId)
                .onSuccess { _effects.send(OutfitEffect.OutfitDeleted) }
                .onFailure { _effects.send(OutfitEffect.ShowError(it.message ?: "Failed to delete")) }
            loadOutfits()
        }
    }

    private fun updateOutfit(outfit: Outfit) {
        viewModelScope.launch {
            repository.updateOutfit(outfit)
                .onSuccess {
                    _effects.send(OutfitEffect.OutfitUpdated)
                    loadOutfits()
                }
                .onFailure {
                    _effects.send(OutfitEffect.ShowError(it.message ?: "Failed to update"))
                }
        }
    }
}
