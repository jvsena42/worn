package com.github.worn.fake

import com.github.worn.domain.model.AgeRange
import com.github.worn.domain.model.BodyType
import com.github.worn.domain.model.Climate
import com.github.worn.domain.model.Lifestyle
import com.github.worn.domain.model.StyleProfile
import com.github.worn.domain.model.UserProfile
import com.github.worn.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSettingsRepository : SettingsRepository {
    val profile = MutableStateFlow(UserProfile())
    val modelPhoto = MutableStateFlow<ByteArray?>(null)

    override fun getUserProfile(): Flow<UserProfile> = profile

    override suspend fun updateBodyType(bodyType: BodyType?): Result<Unit> = Result.success(Unit)
    override suspend fun updateStyleProfile(styleProfile: StyleProfile?): Result<Unit> = Result.success(Unit)
    override suspend fun updateAgeRange(ageRange: AgeRange?): Result<Unit> = Result.success(Unit)
    override suspend fun updateClimate(climate: Climate?): Result<Unit> = Result.success(Unit)
    override suspend fun updateLifestyles(lifestyles: Set<Lifestyle>): Result<Unit> = Result.success(Unit)

    override fun hasModelPhoto(): Flow<Boolean> = modelPhoto.map { it != null }
    override suspend fun saveModelPhoto(bytes: ByteArray): Result<Unit> {
        modelPhoto.value = bytes
        return Result.success(Unit)
    }
    override suspend fun getModelPhoto(): Result<ByteArray?> = Result.success(modelPhoto.value)
    override suspend fun clearModelPhoto(): Result<Unit> {
        modelPhoto.value = null
        return Result.success(Unit)
    }
}
