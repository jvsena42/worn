package com.github.worn.fake

import com.github.worn.domain.model.AgeRange
import com.github.worn.domain.model.BodyType
import com.github.worn.domain.model.Climate
import com.github.worn.domain.model.Lifestyle
import com.github.worn.domain.model.OnDeviceAiAvailability
import com.github.worn.domain.model.OnDeviceAiUnavailableReason
import com.github.worn.domain.model.StyleProfile
import com.github.worn.domain.model.UserProfile
import com.github.worn.domain.model.isUsable
import com.github.worn.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeSettingsRepository : SettingsRepository {
    val profile = MutableStateFlow(UserProfile())
    val modelPhoto = MutableStateFlow<ByteArray?>(null)

    /** Mirrors the real repository's tick, so tests can assign the fields below and see it emit. */
    private val credentialRevision = MutableStateFlow(0)

    var apiKey: String? = null
        set(value) {
            field = value
            credentialRevision.update { it + 1 }
        }
    var youCamClientId: String? = null
        set(value) {
            field = value
            credentialRevision.update { it + 1 }
        }
    var youCamClientSecret: String? = null
        set(value) {
            field = value
            credentialRevision.update { it + 1 }
        }
    val onDeviceAiEnabled = MutableStateFlow(false)
    var onDeviceAiAvailability: OnDeviceAiAvailability =
        OnDeviceAiAvailability.Unavailable(OnDeviceAiUnavailableReason.UNSUPPORTED_DEVICE)

    override fun getUserProfile(): Flow<UserProfile> = profile

    override suspend fun updateBodyType(bodyType: BodyType?): Result<Unit> = Result.success(Unit)
    override suspend fun updateStyleProfile(styleProfile: StyleProfile?): Result<Unit> = Result.success(Unit)
    override suspend fun updateAgeRange(ageRange: AgeRange?): Result<Unit> = Result.success(Unit)
    override suspend fun updateClimate(climate: Climate?): Result<Unit> = Result.success(Unit)
    override suspend fun updateLifestyles(lifestyles: Set<Lifestyle>): Result<Unit> = Result.success(Unit)

    override suspend fun saveModelPhoto(bytes: ByteArray): Result<Unit> {
        modelPhoto.value = bytes
        return Result.success(Unit)
    }
    override suspend fun getModelPhoto(): Result<ByteArray?> = Result.success(modelPhoto.value)
    override suspend fun clearModelPhoto(): Result<Unit> {
        modelPhoto.value = null
        return Result.success(Unit)
    }

    override suspend fun hasApiKey(): Result<Boolean> = Result.success(apiKey != null)

    override suspend fun saveApiKey(key: String): Result<Unit> {
        apiKey = key
        return Result.success(Unit)
    }

    override suspend fun clearApiKey(): Result<Unit> {
        apiKey = null
        return Result.success(Unit)
    }

    override suspend fun hasYouCamCredentials(): Result<Boolean> =
        Result.success(!youCamClientId.isNullOrBlank() && !youCamClientSecret.isNullOrBlank())

    override suspend fun saveYouCamCredentials(
        clientId: String,
        clientSecret: String,
    ): Result<Unit> {
        youCamClientId = clientId
        youCamClientSecret = clientSecret
        return Result.success(Unit)
    }

    override suspend fun clearYouCamCredentials(): Result<Unit> {
        youCamClientId = null
        youCamClientSecret = null
        return Result.success(Unit)
    }

    override fun hasApiKeyFlow(): Flow<Boolean> = credentialRevision
        .map { apiKey != null }
        .distinctUntilChanged()

    override fun hasYouCamCredentialsFlow(): Flow<Boolean> = credentialRevision
        .map { !youCamClientId.isNullOrBlank() && !youCamClientSecret.isNullOrBlank() }
        .distinctUntilChanged()

    override fun isAiAvailableFlow(): Flow<Boolean> =
        combine(credentialRevision, onDeviceAiEnabled) { _, _ -> isAiAvailable().getOrDefault(false) }
            .distinctUntilChanged()

    override fun isOnDeviceAiEnabled(): Flow<Boolean> = onDeviceAiEnabled

    override suspend fun setOnDeviceAiEnabled(enabled: Boolean): Result<Unit> {
        onDeviceAiEnabled.value = enabled
        return Result.success(Unit)
    }

    override suspend fun getOnDeviceAiAvailability(): Result<OnDeviceAiAvailability> =
        Result.success(onDeviceAiAvailability)

    override suspend fun isAiAvailable(): Result<Boolean> = Result.success(
        apiKey != null || (onDeviceAiEnabled.value && onDeviceAiAvailability.isUsable),
    )
}
