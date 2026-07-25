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
import com.github.worn.util.secret.SecretStore
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
    data class SaveModelPhoto(val bytes: ByteArray) : SettingsIntent
    data object ClearModelPhoto : SettingsIntent
}

data class SettingsState(
    val userProfile: UserProfile = UserProfile(),
    val isLoading: Boolean = false,
    val hasApiKey: Boolean = false,
    val hasYouCamKey: Boolean = false,
    val hasModelPhoto: Boolean = false,
    val error: String? = null,
)

sealed interface SettingsEffect {
    data class ShowError(val message: String) : SettingsEffect
    data object ApiKeySaved : SettingsEffect
    data object ApiKeyCleared : SettingsEffect
    data object YouCamCredentialsSaved : SettingsEffect
    data object YouCamCredentialsCleared : SettingsEffect
    data object ModelPhotoSaved : SettingsEffect
    data object ModelPhotoCleared : SettingsEffect
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val secretStore: SecretStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        _state.update {
            it.copy(
                hasApiKey = secretStore.getApiKey() != null,
                hasYouCamKey = hasYouCamCredentials(),
            )
        }
        onIntent(SettingsIntent.LoadProfile)
        viewModelScope.launch {
            settingsRepository.hasModelPhoto().collect { has ->
                _state.update { it.copy(hasModelPhoto = has) }
            }
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
            is SettingsIntent.SaveModelPhoto -> saveModelPhoto(intent.bytes)
            is SettingsIntent.ClearModelPhoto -> clearModelPhoto()
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
        secretStore.saveApiKey(key)
        _state.update { it.copy(hasApiKey = true) }
        viewModelScope.launch {
            _effects.send(SettingsEffect.ApiKeySaved)
        }
    }

    private fun clearApiKey() {
        secretStore.clearApiKey()
        _state.update { it.copy(hasApiKey = false) }
        viewModelScope.launch {
            _effects.send(SettingsEffect.ApiKeyCleared)
        }
    }

    private fun saveYouCamCredentials(clientId: String, clientSecret: String) {
        secretStore.saveSecret(SecretStore.YOUCAM_CLIENT_ID, clientId)
        secretStore.saveSecret(SecretStore.YOUCAM_CLIENT_SECRET, clientSecret)
        _state.update { it.copy(hasYouCamKey = true) }
        viewModelScope.launch {
            _effects.send(SettingsEffect.YouCamCredentialsSaved)
        }
    }

    private fun clearYouCamCredentials() {
        secretStore.clearSecret(SecretStore.YOUCAM_CLIENT_ID)
        secretStore.clearSecret(SecretStore.YOUCAM_CLIENT_SECRET)
        _state.update { it.copy(hasYouCamKey = false) }
        viewModelScope.launch {
            _effects.send(SettingsEffect.YouCamCredentialsCleared)
        }
    }

    private fun saveModelPhoto(bytes: ByteArray) {
        viewModelScope.launch {
            settingsRepository.saveModelPhoto(bytes)
                .onSuccess { _effects.send(SettingsEffect.ModelPhotoSaved) }
                .onFailure { error ->
                    _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to save photo"))
                }
        }
    }

    private fun clearModelPhoto() {
        viewModelScope.launch {
            settingsRepository.clearModelPhoto()
                .onSuccess { _effects.send(SettingsEffect.ModelPhotoCleared) }
                .onFailure { error ->
                    _effects.send(SettingsEffect.ShowError(error.message ?: "Failed to remove photo"))
                }
        }
    }

    private fun hasYouCamCredentials(): Boolean =
        !secretStore.getSecret(SecretStore.YOUCAM_CLIENT_ID).isNullOrBlank() &&
            !secretStore.getSecret(SecretStore.YOUCAM_CLIENT_SECRET).isNullOrBlank()
}
