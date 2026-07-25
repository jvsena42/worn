package com.github.worn.domain.repository

import com.github.worn.domain.model.AgeRange
import com.github.worn.domain.model.BodyType
import com.github.worn.domain.model.Climate
import com.github.worn.domain.model.Lifestyle
import com.github.worn.domain.model.StyleProfile
import com.github.worn.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getUserProfile(): Flow<UserProfile>
    suspend fun updateBodyType(bodyType: BodyType?): Result<Unit>
    suspend fun updateStyleProfile(styleProfile: StyleProfile?): Result<Unit>
    suspend fun updateAgeRange(ageRange: AgeRange?): Result<Unit>
    suspend fun updateClimate(climate: Climate?): Result<Unit>
    suspend fun updateLifestyles(lifestyles: Set<Lifestyle>): Result<Unit>

    /** Emits whether a reusable "model photo" (used for virtual try-on) is saved. */
    fun hasModelPhoto(): Flow<Boolean>
    suspend fun saveModelPhoto(bytes: ByteArray): Result<Unit>
    suspend fun getModelPhoto(): Result<ByteArray?>
    suspend fun clearModelPhoto(): Result<Unit>
}
