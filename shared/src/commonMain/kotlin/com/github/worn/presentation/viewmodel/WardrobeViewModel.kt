package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Season
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

sealed interface WardrobeIntent {
    data object LoadItems : WardrobeIntent
    data class FilterByCategory(val category: Category?) : WardrobeIntent
    data class AddItem(
        val imageBytes: ByteArray,
        val name: String,
        val category: Category,
        val colors: List<String>,
        val seasons: List<Season>,
    ) : WardrobeIntent
    data class ToggleSelection(val itemId: String) : WardrobeIntent
    data object ClearSelection : WardrobeIntent
    data object DeleteSelected : WardrobeIntent
}

data class WardrobeState(
    val items: List<ClothingItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val activeCategory: Category? = null,
    val hasApiKey: Boolean = false,
    val error: String? = null,
)

sealed interface WardrobeEffect {
    data class ShowError(val message: String) : WardrobeEffect
    data object ItemAdded : WardrobeEffect
    data object ItemsDeleted : WardrobeEffect
}

class WardrobeViewModel(
    private val repository: WardrobeRepository,
    private val secretStore: SecretStore,
) : ViewModel() {

    private val _state = MutableStateFlow(WardrobeState())
    val state: StateFlow<WardrobeState> = _state.asStateFlow()

    private val _effects = Channel<WardrobeEffect>(Channel.BUFFERED)
    val effects: Flow<WardrobeEffect> = _effects.receiveAsFlow()

    init {
        refreshApiKeyState()
        onIntent(WardrobeIntent.LoadItems)
    }

    private fun refreshApiKeyState() {
        _state.update { it.copy(hasApiKey = secretStore.getApiKey() != null) }
    }

    fun onIntent(intent: WardrobeIntent) {
        when (intent) {
            is WardrobeIntent.LoadItems -> loadItems()
            is WardrobeIntent.FilterByCategory -> filterByCategory(intent.category)
            is WardrobeIntent.AddItem -> addItem(intent)
            is WardrobeIntent.ToggleSelection -> toggleSelection(intent.itemId)
            is WardrobeIntent.ClearSelection -> clearSelection()
            is WardrobeIntent.DeleteSelected -> deleteSelected()
        }
    }

    private fun loadItems() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = when (val cat = _state.value.activeCategory) {
                null -> repository.getAll()
                else -> repository.getByCategory(cat)
            }
            result.onSuccess { items ->
                _state.update { it.copy(items = items, isLoading = false) }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
                _effects.send(WardrobeEffect.ShowError(error.message ?: "Unknown error"))
            }
        }
    }

    private fun filterByCategory(category: Category?) {
        _state.update { it.copy(activeCategory = category) }
        loadItems()
    }

    private fun addItem(intent: WardrobeIntent.AddItem) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            repository.addItem(
                imageBytes = intent.imageBytes,
                name = intent.name,
                category = intent.category,
                colors = intent.colors,
                seasons = intent.seasons,
            ).onSuccess {
                _state.update { it.copy(isSaving = false) }
                _effects.send(WardrobeEffect.ItemAdded)
                loadItems()
            }.onFailure { error ->
                _state.update { it.copy(isSaving = false) }
                _effects.send(WardrobeEffect.ShowError(error.message ?: "Failed to save"))
            }
        }
    }

    private fun toggleSelection(itemId: String) {
        _state.update { state ->
            val updated = if (itemId in state.selectedIds) {
                state.selectedIds - itemId
            } else {
                state.selectedIds + itemId
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
                    items = state.items.filterNot { it.id in ids },
                    selectedIds = emptySet(),
                )
            }
            var failed = false
            for (id in ids) {
                repository.deleteItem(id).onFailure { failed = true }
            }
            _state.update { it.copy(isDeleting = false) }
            if (failed) {
                _effects.send(WardrobeEffect.ShowError("Some items could not be deleted"))
            } else {
                _effects.send(WardrobeEffect.ItemsDeleted)
            }
            loadItems()
        }
    }
}
