package com.github.worn.fake

import com.github.worn.data.source.ai.OnDeviceAiEngine
import com.github.worn.domain.model.OnDeviceAiAvailability
import com.github.worn.domain.model.OnDeviceAiUnavailableReason

class FakeOnDeviceAiEngine : OnDeviceAiEngine {
    var availability: OnDeviceAiAvailability =
        OnDeviceAiAvailability.Unavailable(OnDeviceAiUnavailableReason.UNSUPPORTED_DEVICE)

    /** Returned verbatim by [generate], so tests can feed fenced or malformed replies. */
    var response: String = "{}"
    var failure: Throwable? = null

    var lastSystemPrompt: String? = null
        private set
    var lastUserText: String? = null
        private set
    var lastImageBytes: ByteArray? = null
        private set
    var generateCount: Int = 0
        private set

    override suspend fun availability(): OnDeviceAiAvailability = availability

    override suspend fun generate(
        systemPrompt: String,
        userText: String,
        imageBytes: ByteArray?,
    ): String {
        generateCount++
        lastSystemPrompt = systemPrompt
        lastUserText = userText
        lastImageBytes = imageBytes
        failure?.let { throw it }
        return response
    }
}
