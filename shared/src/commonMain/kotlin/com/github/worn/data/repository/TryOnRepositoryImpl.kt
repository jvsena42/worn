package com.github.worn.data.repository

import com.github.worn.data.source.image.AiImageLimits
import com.github.worn.data.source.image.ImageDownscaler
import com.github.worn.data.source.remote.YouCamApiClient
import com.github.worn.domain.model.GarmentCategory
import com.github.worn.domain.repository.SettingsRepository
import com.github.worn.domain.repository.TryOnRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class TryOnRepositoryImpl(
    private val youCamClient: YouCamApiClient,
    private val settingsRepository: SettingsRepository,
    private val imageDownscaler: ImageDownscaler,
    private val dispatcher: CoroutineContext,
) : TryOnRepository {

    override suspend fun generateTryOn(
        garmentBytes: ByteArray,
        category: GarmentCategory,
    ): Result<ByteArray> = runCatching {
        withContext(dispatcher) {
            val personBytes = settingsRepository.getModelPhoto().getOrThrow()
                ?: error("Add a photo of yourself to try items on.")
            // Bounded higher than the analysis path: try-on returns a generated image, so detail
            // lost on the way in shows up in the result. This also normalises a HEIC pick to real
            // JPEG, which the upload declares but never checked.
            youCamClient.tryOn(
                personBytes = imageDownscaler.downscale(personBytes, AiImageLimits.TRY_ON_MAX_EDGE),
                garmentBytes = imageDownscaler.downscale(garmentBytes, AiImageLimits.TRY_ON_MAX_EDGE),
                category = category,
            )
        }
    }

    override suspend fun verifyCredentials(clientId: String, clientSecret: String): Result<Unit> =
        runCatching {
            withContext(dispatcher) {
                youCamClient.verifyCredentials(clientId, clientSecret)
            }
        }
}
