package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
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

sealed interface WardrobeIntent {
    data class FilterByCategory(val category: Category?) : WardrobeIntent
    data class AddItem(
        val imageBytes: ByteArray,
        val name: String,
        val category: Category,
        val colors: List<String>,
        val seasons: List<Season>,
        val subcategory: Subcategory? = null,
        val fit: Fit? = null,
        val material: Material? = null,
    ) : WardrobeIntent
    data class ToggleSelection(val itemId: String) : WardrobeIntent
    data object ClearSelection : WardrobeIntent
    data object DeleteSelected : WardrobeIntent
    data class DeleteItem(val itemId: String) : WardrobeIntent
    data class UpdateItem(val item: ClothingItem) : WardrobeIntent
}

data class WardrobeState(
    val items: List<ClothingItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val activeCategory: Category? = null,
    /** A Claude key is configured, or on-device AI is enabled and available. */
    val isAiAvailable: Boolean = false,
    val error: String? = null,
    val totalItemCount: Int = 0,
)

sealed interface WardrobeEffect {
    data class ShowError(val message: String) : WardrobeEffect
    data object ItemAdded : WardrobeEffect
    data object ItemsDeleted : WardrobeEffect
    data object ItemDeleted : WardrobeEffect
    data object ItemUpdated : WardrobeEffect
}

class WardrobeViewModel(
    private val repository: WardrobeRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** The parts of [WardrobeState] not derived from the database. */
    private val _uiState = MutableStateFlow(WardrobeState(isLoading = true))

    private val _effects = Channel<WardrobeEffect>(Channel.BUFFERED)
    val effects: Flow<WardrobeEffect> = _effects.receiveAsFlow()

    /**
     * Mutations never re-query; the DB emission drives the update. [SharingStarted.Eagerly] keeps
     * the last value cached, so re-entering the screen renders immediately rather than behind a
     * spinner.
     */
    val state: StateFlow<WardrobeState> = combine(
        repository.observeAll().catch { error ->
            _uiState.update { it.copy(error = error.message) }
            _effects.send(WardrobeEffect.ShowError(error.message ?: "Unknown error"))
            emit(emptyList())
        },
        _uiState,
    ) { items, ui ->
        ui.copy(
            items = ui.activeCategory?.let { cat -> items.filter { it.category == cat } } ?: items,
            totalItemCount = items.size,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WardrobeState(isLoading = true))

    init {
        observeAiAvailability()
    }

    /** Collected rather than read once: a key added on Settings must unlock this screen live. */
    private fun observeAiAvailability() {
        viewModelScope.launch {
            settingsRepository.isAiAvailableFlow().collect { isAiAvailable ->
                _uiState.update { it.copy(isAiAvailable = isAiAvailable) }
            }
        }
    }

    fun onIntent(intent: WardrobeIntent) {
        when (intent) {
            is WardrobeIntent.FilterByCategory -> filterByCategory(intent.category)
            is WardrobeIntent.AddItem -> addItem(intent)
            is WardrobeIntent.ToggleSelection -> toggleSelection(intent.itemId)
            is WardrobeIntent.ClearSelection -> clearSelection()
            is WardrobeIntent.DeleteSelected -> deleteSelected()
            is WardrobeIntent.DeleteItem -> deleteItem(intent.itemId)
            is WardrobeIntent.UpdateItem -> updateItem(intent.item)
        }
    }

    private fun filterByCategory(category: Category?) {
        _uiState.update { it.copy(activeCategory = category) }
    }

    private fun addItem(intent: WardrobeIntent.AddItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.addItem(
                imageBytes = intent.imageBytes,
                name = intent.name,
                category = intent.category,
                colors = intent.colors,
                seasons = intent.seasons,
                subcategory = intent.subcategory,
                fit = intent.fit,
                material = intent.material,
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(WardrobeEffect.ItemAdded)
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(WardrobeEffect.ShowError(error.message ?: "Failed to save"))
            }
        }
    }

    private fun toggleSelection(itemId: String) {
        _uiState.update { state ->
            val updated = if (itemId in state.selectedIds) {
                state.selectedIds - itemId
            } else {
                state.selectedIds + itemId
            }
            state.copy(selectedIds = updated)
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    private fun deleteSelected() {
        val ids = state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, selectedIds = emptySet()) }
            var failed = false
            for (id in ids) {
                repository.deleteItem(id).onFailure { failed = true }
            }
            _uiState.update { it.copy(isDeleting = false) }
            if (failed) {
                _effects.send(WardrobeEffect.ShowError("Some items could not be deleted"))
            } else {
                _effects.send(WardrobeEffect.ItemsDeleted)
            }
        }
    }

    private fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
                .onSuccess { _effects.send(WardrobeEffect.ItemDeleted) }
                .onFailure { _effects.send(WardrobeEffect.ShowError(it.message ?: "Failed to delete")) }
        }
    }

    private fun updateItem(item: ClothingItem) {
        viewModelScope.launch {
            repository.updateItem(item)
                .onSuccess { _effects.send(WardrobeEffect.ItemUpdated) }
                .onFailure {
                    _effects.send(WardrobeEffect.ShowError(it.message ?: "Failed to update"))
                }
        }
    }
}
