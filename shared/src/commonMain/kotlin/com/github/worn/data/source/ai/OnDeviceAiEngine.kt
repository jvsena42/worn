package com.github.worn.data.source.ai

import com.github.worn.domain.model.OnDeviceAiAvailability

/**
 * The platform's built-in language model: Gemini Nano via ML Kit GenAI on Android, Apple
 * Intelligence via `FoundationModels` on iOS.
 *
 * A plain interface with per-platform implementations bound in Koin (the [SecretStore][
 * com.github.worn.util.secret.SecretStore] idiom) rather than an `expect class` (the
 * [BackgroundRemover][com.github.worn.data.source.image.BackgroundRemover] idiom), because
 * [OnDeviceAiSource] holds real prompt and parsing logic that has to be testable in `commonTest`
 * against a fake — which an `expect class` cannot provide.
 *
 * Deliberately a thin text-in/text-out primitive mirroring `ClaudeApiClient.sendRequest`: prompts
 * ([AiPrompts]) and parsing ([AiResponseParser]) stay shared so both providers behave alike.
 */
interface OnDeviceAiEngine {

    suspend fun availability(): OnDeviceAiAvailability

    /**
     * Runs [systemPrompt] plus [userText] and optional JPEG [imageBytes], returning the model's
     * raw text. Throws with a user-facing message when the model is unavailable or inference
     * fails — there is no cloud fallback, so the error surfaces to the caller as-is.
     */
    suspend fun generate(
        systemPrompt: String,
        userText: String,
        imageBytes: ByteArray? = null,
    ): String
}
