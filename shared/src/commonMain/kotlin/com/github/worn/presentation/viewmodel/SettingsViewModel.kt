package com.github.worn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.worn.domain.model.AgeRange
import com.github.worn.domain.model.BodyType
import com.github.worn.domain.model.Climate
import com.github.worn.domain.model.Lifestyle
import com.github.worn.domain.model.StyleProfile
import com.github.worn.domain.model.UserProfile
import com.github.worn.domain.repository.SettingsRepository
import com.github.worn.domain.repository.TryOnRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SettingsIntent {
    data object LoadProfile : SettingsIntent
    data class SelectBodyType(val bodyType: BodyType?) : SettingsIntent
    data class SelectStyleProfile(val styleProfile: StyleProfile?) : SettingsIntent
    data class SelectAgeRange(val ageRange: AgeRange?) : SettingsIntent
    data class SelectClimate(val climate: Climate?) : SettingsIntent
    data class ToggleLifestyle(val lifestyle: Lifestyle) : SettingsIntent
    data class SaveApiKey(val key: String) : SettingsIntent
    data object ClearApiKey : SettingsIntent
    data class SaveYouCamCredentials(val clientId: String, val clientSecret: String) : SettingsIntent
    data object ClearYouCamCredentials : SettingsIntent
}

data class SettingsState(
    val userProfile: UserProfile = UserProfile(),
    val isLoading: Boolean = false,
    val hasApiKey: Boolean = false,
    val hasYouCamKey: Boolean = false,
    val verifyingYouCam: Boolean = false,
    val youCamError: String? = null,
    val error: String? = null,
)

sealed interface SettingsEffect {
    data class ShowError(val message: String) : SettingsEffect
    data object ApiKeySaved : SettingsEffect
    data object ApiKeyCleared : SettingsEffect
    data object YouCamCredentialsSaved : SettingsEffect
    data object YouCamCredentialsCleared : SettingsEffect
}

@Suppress("TooManyFunctions")
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val tryOnRepository: TryOnRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        refreshCredentialState()
        onIntent(SettingsIntent.LoadProfile)
    }

    private fun refreshCredentialState() {
        viewModelScope.launch {
            val hasApiKey = settingsRepository.hasApiKey().getOrDefault(false)
            val hasYouCamKey = settingsRepository.hasYouCamCredentials().getOrDefault(false)
            _state.update { it.copy(hasApiKey = hasApiKey, hasYouCamKey = hasYouCamKey) }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.LoadProfile -> loadProfile()
            is SettingsIntent.SelectBodyType -> updateBodyType(intent.bodyType)
            is SettingsIntent.SelectStyleProfile -> updateStyleProfile(intent.styleProfile)
            is SettingsIntent.SelectAgeRange -> updateAgeRange(intent.ageRange)
            is SettingsIntent.SelectClimate -> updateClimate(intent.climate)
            is SettingsIntent.ToggleLifestyle -> toggleLifestyle(intent.lifestyle)
            is SettingsIntent.SaveApiKey -> saveApiKey(intent.key)
            is SettingsIntent.ClearApiKey -> clearApiKey()
            is SettingsIntent.SaveYouCamCredentials -> saveYouCamCredentials(intent.clientId, intent.clientSecret)
            is SettingsIntent.ClearYouCamCredentials -> clearYouCamCredentials()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            settingsRepository.getUserProfile().collect { profile ->
                _state.update { it.copy(userProfile = profile, isLoading = false) }
            }
        }
    }

    private fun updateBodyType(bodyType: BodyType?) {
        viewModelScope.launch {
            settingsRepository.updateBodyType(bodyType).onFailure { error ->
                _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to save"))
            }
        }
    }

    private fun updateStyleProfile(styleProfile: StyleProfile?) {
        viewModelScope.launch {
            settingsRepository.updateStyleProfile(styleProfile).onFailure { error ->
                _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to save"))
            }
        }
    }

    private fun updateAgeRange(ageRange: AgeRange?) {
        viewModelScope.launch {
            settingsRepository.updateAgeRange(ageRange).onFailure { error ->
                _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to save"))
            }
        }
    }

    private fun updateClimate(climate: Climate?) {
        viewModelScope.launch {
            settingsRepository.updateClimate(climate).onFailure { error ->
                _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to save"))
            }
        }
    }

    private fun toggleLifestyle(lifestyle: Lifestyle) {
        viewModelScope.launch {
            val current = _state.value.userProfile.lifestyles
            val updated = if (lifestyle in current) current - lifestyle else current + lifestyle
            settingsRepository.updateLifestyles(updated).onFailure { error ->
                _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to save"))
            }
        }
    }

    private fun saveApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(key)
                .onSuccess {
                    _state.update { it.copy(hasApiKey = true) }
                    _effects.send(SettingsEffect.ApiKeySaved)
                }
                .onFailure { error ->
                    _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to save"))
                }
        }
    }

    private fun clearApiKey() {
        viewModelScope.launch {
            settingsRepository.clearApiKey()
                .onSuccess {
                    _state.update { it.copy(hasApiKey = false) }
                    _effects.send(SettingsEffect.ApiKeyCleared)
                }
                .onFailure { error ->
                    _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to clear"))
                }
        }
    }

    private fun saveYouCamCredentials(clientId: String, clientSecret: String) {
        _state.update { it.copy(verifyingYouCam = true, youCamError = null) }
        viewModelScope.launch {
            tryOnRepository.verifyCredentials(clientId, clientSecret)
                .mapCatching {
                    settingsRepository.saveYouCamCredentials(clientId, clientSecret).getOrThrow()
                }
                .onSuccess {
                    _state.update { it.copy(hasYouCamKey = true, verifyingYouCam = false) }
                    _effects.send(SettingsEffect.YouCamCredentialsSaved)
                }
                .onFailure { error ->
                    val message = error.message ?: "Could not verify credentials"
                    _state.update { it.copy(verifyingYouCam = false, youCamError = message) }
                    _effects.send(SettingsEffect.ShowError(message))
                }
        }
    }

    private fun clearYouCamCredentials() {
        viewModelScope.launch {
            settingsRepository.clearYouCamCredentials()
                .onSuccess {
                    _state.update { it.copy(hasYouCamKey = false) }
                    _effects.send(SettingsEffect.YouCamCredentialsCleared)
                }
                .onFailure { error ->
                    _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to clear"))
                }
        }
    }
}
