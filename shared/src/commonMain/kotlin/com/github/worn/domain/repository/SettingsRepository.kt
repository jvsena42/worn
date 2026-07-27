package com.github.worn.domain.repository

import com.github.worn.domain.model.AgeRange
import com.github.worn.domain.model.BodyType
import com.github.worn.domain.model.Climate
import com.github.worn.domain.model.Lifestyle
import com.github.worn.domain.model.OnDeviceAiAvailability
import com.github.worn.domain.model.StyleProfile
import com.github.worn.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface SettingsRepository {
    fun getUserProfile(): Flow<UserProfile>
    suspend fun updateBodyType(bodyType: BodyType?): Result<Unit>
    suspend fun updateStyleProfile(styleProfile: StyleProfile?): Result<Unit>
    suspend fun updateAgeRange(ageRange: AgeRange?): Result<Unit>
    suspend fun updateClimate(climate: Climate?): Result<Unit>
    suspend fun updateLifestyles(lifestyles: Set<Lifestyle>): Result<Unit>

    /** The reusable "person photo" used for virtual try-on, entered from the Try-It screen. */
    suspend fun saveModelPhoto(bytes: ByteArray): Result<Unit>
    suspend fun getModelPhoto(): Result<ByteArray?>
    suspend fun clearModelPhoto(): Result<Unit>

    // BYOK credentials. The platform secret store blocks (Keystore decrypt, Keychain access), so
    // these are suspend and dispatched here rather than at the call site.
    suspend fun hasApiKey(): Result<Boolean>
    suspend fun saveApiKey(key: String): Result<Unit>
    suspend fun clearApiKey(): Result<Unit>
    suspend fun hasYouCamCredentials(): Result<Boolean>
    suspend fun saveYouCamCredentials(clientId: String, clientSecret: String): Result<Unit>
    suspend fun clearYouCamCredentials(): Result<Unit>

    /** Whether the user has opted into running AI on the device instead of calling Claude. */
    fun isOnDeviceAiEnabled(): Flow<Boolean>
    suspend fun setOnDeviceAiEnabled(enabled: Boolean): Result<Unit>

    /** Whether this device can run the on-device model at all. Queries the platform each time. */
    suspend fun getOnDeviceAiAvailability(): Result<OnDeviceAiAvailability>

    /**
     * Whether *any* AI provider can serve a request: a Claude key is configured, or on-device AI
     * is both enabled and available. Gates the AI features in Wardrobe and Gaps.
     */
    suspend fun isAiAvailable(): Result<Boolean>
}
