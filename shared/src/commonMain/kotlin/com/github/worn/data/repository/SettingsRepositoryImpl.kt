package com.github.worn.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.worn.domain.model.AgeRange
import com.github.worn.domain.model.BodyType
import com.github.worn.domain.model.Climate
import com.github.worn.domain.model.Lifestyle
import com.github.worn.domain.model.StyleProfile
import com.github.worn.domain.model.UserProfile
import com.github.worn.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val dispatcher: CoroutineContext,
) : SettingsRepository {

    override fun getUserProfile(): Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            bodyType = prefs[KEY_BODY_TYPE]?.let {
                runCatching { BodyType.valueOf(it) }.getOrNull()
            },
            styleProfile = prefs[KEY_STYLE_PROFILE]?.let {
                runCatching { StyleProfile.valueOf(it) }.getOrNull()
            },
            ageRange = prefs[KEY_AGE_RANGE]?.let {
                runCatching { AgeRange.valueOf(it) }.getOrNull()
            },
            climate = prefs[KEY_CLIMATE]?.let {
                runCatching { Climate.valueOf(it) }.getOrNull()
            },
            lifestyles = prefs[KEY_LIFESTYLES]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { runCatching { Lifestyle.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: emptySet(),
        )
    }

    override suspend fun updateBodyType(bodyType: BodyType?): Result<Unit> = runCatching {
        withContext(dispatcher) {
            dataStore.edit { prefs ->
                if (bodyType != null) {
                    prefs[KEY_BODY_TYPE] = bodyType.name
                } else {
                    prefs.remove(KEY_BODY_TYPE)
                }
            }
        }
    }

    override suspend fun updateStyleProfile(styleProfile: StyleProfile?): Result<Unit> =
        runCatching {
            withContext(dispatcher) {
                dataStore.edit { prefs ->
                    if (styleProfile != null) {
                        prefs[KEY_STYLE_PROFILE] = styleProfile.name
                    } else {
                        prefs.remove(KEY_STYLE_PROFILE)
                    }
                }
            }
        }

    override suspend fun updateAgeRange(ageRange: AgeRange?): Result<Unit> = runCatching {
        withContext(dispatcher) {
            dataStore.edit { prefs ->
                if (ageRange != null) {
                    prefs[KEY_AGE_RANGE] = ageRange.name
                } else {
                    prefs.remove(KEY_AGE_RANGE)
                }
            }
        }
    }

    override suspend fun updateClimate(climate: Climate?): Result<Unit> = runCatching {
        withContext(dispatcher) {
            dataStore.edit { prefs ->
                if (climate != null) {
                    prefs[KEY_CLIMATE] = climate.name
                } else {
                    prefs.remove(KEY_CLIMATE)
                }
            }
        }
    }

    override suspend fun updateLifestyles(lifestyles: Set<Lifestyle>): Result<Unit> = runCatching {
        withContext(dispatcher) {
            dataStore.edit { prefs ->
                if (lifestyles.isNotEmpty()) {
                    prefs[KEY_LIFESTYLES] = lifestyles.joinToString(",") { it.name }
                } else {
                    prefs.remove(KEY_LIFESTYLES)
                }
            }
        }
    }

    companion object {
        private val KEY_BODY_TYPE = stringPreferencesKey("body_type")
        private val KEY_STYLE_PROFILE = stringPreferencesKey("style_profile")
        private val KEY_AGE_RANGE = stringPreferencesKey("age_range")
        private val KEY_CLIMATE = stringPreferencesKey("climate")
        private val KEY_LIFESTYLES = stringPreferencesKey("lifestyles")
    }
}
