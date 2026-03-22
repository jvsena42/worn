package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.repository.WardrobeRepository
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
}

data class WardrobeState(
    val items: List<ClothingItem> = emptyList(),
    val isLoading: Boolean = false,
    val activeCategory: Category? = null,
    val error: String? = null,
)

sealed interface WardrobeEffect {
    data class ShowError(val message: String) : WardrobeEffect
}

class WardrobeViewModel(
    private val repository: WardrobeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WardrobeState())
    val state: StateFlow<WardrobeState> = _state.asStateFlow()

    private val _effects = Channel<WardrobeEffect>(Channel.BUFFERED)
    val effects: Flow<WardrobeEffect> = _effects.receiveAsFlow()

    init {
        onIntent(WardrobeIntent.LoadItems)
    }

    fun onIntent(intent: WardrobeIntent) {
        when (intent) {
            is WardrobeIntent.LoadItems -> loadItems()
            is WardrobeIntent.FilterByCategory -> filterByCategory(intent.category)
        }
    }

    private fun loadItems() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                when (val cat = _state.value.activeCategory) {
                    null -> repository.getAll()
                    else -> repository.getByCategory(cat)
                }
            }.onSuccess { items ->
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
}
