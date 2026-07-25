package com.github.worn.data.repository

import com.github.worn.data.source.remote.YouCamApiClient
import com.github.worn.domain.model.GarmentCategory
import com.github.worn.domain.repository.SettingsRepository
import com.github.worn.domain.repository.TryOnRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class TryOnRepositoryImpl(
    private val youCamClient: YouCamApiClient,
    private val settingsRepository: SettingsRepository,
    private val dispatcher: CoroutineContext,
) : TryOnRepository {

    override suspend fun generateTryOn(
        garmentBytes: ByteArray,
        category: GarmentCategory,
    ): Result<ByteArray> = runCatching {
        withContext(dispatcher) {
            val personBytes = settingsRepository.getModelPhoto().getOrThrow()
                ?: error("Save a model photo in Settings to try items on.")
            youCamClient.tryOn(personBytes, garmentBytes, category)
        }
    }
}
