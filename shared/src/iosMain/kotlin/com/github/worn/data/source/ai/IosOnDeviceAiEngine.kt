package com.github.worn.data.source.ai

import com.github.worn.domain.model.OnDeviceAiAvailability
import com.github.worn.domain.model.OnDeviceAiUnavailableReason
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * Apple Intelligence via `FoundationModels`, reached through the Swift [OnDeviceAiBridge].
 *
 * When no bridge is registered the app is running on an OS older than the framework requires
 * (the Swift side guards with `#available`), so that reports as [UNSUPPORTED_OS][
 * OnDeviceAiUnavailableReason.UNSUPPORTED_OS] rather than an error.
 */
class IosOnDeviceAiEngine(
    private val dispatcher: CoroutineContext,
) : OnDeviceAiEngine {

    override suspend fun availability(): OnDeviceAiAvailability = withContext(dispatcher) {
        val bridge = OnDeviceAiBridgeRegistry.bridge
            ?: return@withContext OnDeviceAiAvailability.Unavailable(
                OnDeviceAiUnavailableReason.UNSUPPORTED_OS,
            )
        val token = suspendCancellableCoroutine { continuation ->
            bridge.availability { continuation.resume(it) }
        }
        token.toAvailability()
    }

    private fun OnDeviceAiAvailabilityToken.toAvailability(): OnDeviceAiAvailability = when (this) {
        OnDeviceAiAvailabilityToken.AVAILABLE -> OnDeviceAiAvailability.Available
        OnDeviceAiAvailabilityToken.DOWNLOADABLE -> OnDeviceAiAvailability.Downloadable
        OnDeviceAiAvailabilityToken.UNSUPPORTED_DEVICE ->
            OnDeviceAiAvailability.Unavailable(OnDeviceAiUnavailableReason.UNSUPPORTED_DEVICE)
        OnDeviceAiAvailabilityToken.UNSUPPORTED_OS ->
            OnDeviceAiAvailability.Unavailable(OnDeviceAiUnavailableReason.UNSUPPORTED_OS)
        OnDeviceAiAvailabilityToken.DISABLED_BY_USER ->
            OnDeviceAiAvailability.Unavailable(OnDeviceAiUnavailableReason.DISABLED_BY_USER)
        OnDeviceAiAvailabilityToken.UNKNOWN ->
            OnDeviceAiAvailability.Unavailable(OnDeviceAiUnavailableReason.UNKNOWN)
    }

    override suspend fun generate(
        systemPrompt: String,
        userText: String,
        imageBytes: ByteArray?,
    ): String = withContext(dispatcher) {
        val bridge = OnDeviceAiBridgeRegistry.bridge
            ?: error("On-device AI isn't available on this device. Turn it off in Settings.")
        val result = suspendCancellableCoroutine { continuation ->
            bridge.generate(systemPrompt, userText, imageBytes) { response, errorMessage ->
                continuation.resume(response to errorMessage)
            }
        }
        val (response, errorMessage) = result
        if (errorMessage != null) error(errorMessage)
        response?.takeIf { it.isNotBlank() }
            ?: error("On-device AI returned an empty response. Please try again.")
    }
}
