package com.github.worn.data.source.ai

import com.github.worn.domain.model.OnDeviceAiAvailability
import com.github.worn.domain.model.OnDeviceAiUnavailableReason
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Gemini Nano through ML Kit GenAI, brokered by the system AICore service.
 *
 * The model ships with the OS rather than the app, so availability is a device property that
 * ML Kit reports directly — no multi-gigabyte download to manage, unlike a bundled Gemma via
 * MediaPipe. That check is what lets Settings disable the toggle on unsupported hardware.
 */
class AndroidOnDeviceAiEngine(
    private val dispatcher: CoroutineContext,
) : OnDeviceAiEngine {

    override suspend fun availability(): OnDeviceAiAvailability = withContext(dispatcher) {
        runCatching { withClient { it.checkStatus() } }
            .fold(
                onSuccess = { status ->
                    when (status) {
                        // Already downloading: it will be ready, so let the user opt in now.
                        FeatureStatus.AVAILABLE, FeatureStatus.DOWNLOADING ->
                            OnDeviceAiAvailability.Available
                        FeatureStatus.DOWNLOADABLE -> OnDeviceAiAvailability.Downloadable
                        else -> OnDeviceAiAvailability.Unavailable(
                            OnDeviceAiUnavailableReason.UNSUPPORTED_DEVICE,
                        )
                    }
                },
                onFailure = { OnDeviceAiAvailability.Unavailable(it.toReason()) },
            )
    }

    override suspend fun generate(
        systemPrompt: String,
        userText: String,
        imageBytes: ByteArray?,
    ): String = withContext(dispatcher) {
        withClient { model ->
            model.ensureModelDownloaded()

            val configure: GenerateContentRequest.Builder.() -> Unit = {
                // The prefix is the system-instruction slot; keeping it separate from the user
                // text lets AICore cache it across calls.
                promptPrefix = PromptPrefix(systemPrompt)
                // Near-greedy decoding: these prompts want one exact JSON shape, not variety.
                temperature = TEMPERATURE
                candidateCount = 1
                maxOutputTokens = MAX_OUTPUT_TOKENS
            }

            val request = if (imageBytes != null) {
                generateContentRequest(ImagePart(imageBytes), TextPart(userText), configure)
            } else {
                generateContentRequest(TextPart(userText), configure)
            }

            val response = model.generateContent(request)
            response.candidates.firstOrNull()?.text?.takeIf { it.isNotBlank() }
                ?: error("On-device AI returned an empty response. Please try again.")
        }
    }

    private suspend fun GenerativeModel.ensureModelDownloaded() {
        when (checkStatus()) {
            FeatureStatus.AVAILABLE -> return
            FeatureStatus.UNAVAILABLE ->
                error("On-device AI isn't available on this device. Turn it off in Settings.")
        }
        download().collect { status ->
            if (status is DownloadStatus.DownloadFailed) {
                throw IllegalStateException(
                    "Could not download the on-device AI model. ${status.e.message.orEmpty()}".trim(),
                    status.e,
                )
            }
        }
    }

    /**
     * A client holds an AICore connection, so it is opened per operation and always closed.
     * `try`/`finally` rather than `runCatching` because the close has to happen on both paths.
     */
    private suspend fun <T> withClient(block: suspend (GenerativeModel) -> T): T {
        val model = Generation.getClient()
        return try {
            block(model)
        } finally {
            model.close()
        }
    }

    private fun Throwable.toReason(): OnDeviceAiUnavailableReason = when {
        this !is GenAiException -> OnDeviceAiUnavailableReason.UNKNOWN
        errorCode == GenAiException.ErrorCode.AICORE_INCOMPATIBLE ->
            OnDeviceAiUnavailableReason.UNSUPPORTED_DEVICE
        errorCode == GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE ->
            OnDeviceAiUnavailableReason.UNSUPPORTED_OS
        else -> OnDeviceAiUnavailableReason.UNKNOWN
    }

    private companion object {
        const val TEMPERATURE = 0.1f
        const val MAX_OUTPUT_TOKENS = 1024
    }
}
