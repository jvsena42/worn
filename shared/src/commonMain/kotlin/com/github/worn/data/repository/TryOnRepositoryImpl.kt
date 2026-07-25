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
                ?: error("Add a photo of yourself to try items on.")
            youCamClient.tryOn(personBytes, garmentBytes, category)
        }
    }

    override suspend fun verifyCredentials(clientId: String, clientSecret: String): Result<Unit> =
        runCatching {
            withContext(dispatcher) {
                youCamClient.verifyCredentials(clientId, clientSecret)
            }
        }
}
