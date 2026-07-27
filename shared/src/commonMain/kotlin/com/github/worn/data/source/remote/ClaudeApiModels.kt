package com.github.worn.data.source.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ClaudeMessage>,
    val thinking: ClaudeThinking = ClaudeThinking(),
)

/**
 * Thinking is on by default from Claude Sonnet 5 onward, and `max_tokens` caps thinking *plus*
 * response text. These calls parse the reply as JSON, so thinking is disabled to keep the whole
 * budget available for the answer.
 */
@Serializable
internal data class ClaudeThinking(
    val type: String = "disabled",
)

@Serializable
internal data class ClaudeMessage(
    val role: String,
    val content: List<ClaudeContent>,
)

@Serializable
internal data class ClaudeContent(
    val type: String,
    val text: String? = null,
    val source: ClaudeImageSource? = null,
)

@Serializable
internal data class ClaudeImageSource(
    val type: String = "base64",
    @SerialName("media_type") val mediaType: String = "image/jpeg",
    val data: String,
)

@Serializable
internal data class ClaudeResponse(
    val content: List<ClaudeResponseContent>,
)

@Serializable
internal data class ClaudeResponseContent(
    val type: String,
    val text: String? = null,
)

@Serializable
internal data class ClaudeErrorResponse(
    val type: String,
    val error: ClaudeErrorDetail,
)

@Serializable
internal data class ClaudeErrorDetail(
    val type: String,
    val message: String,
)
