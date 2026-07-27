package com.github.worn.data.source.remote

import com.github.worn.data.source.ai.AiPrompts
import com.github.worn.data.source.ai.AiPrompts.toPromptContext
import com.github.worn.data.source.ai.AiResponseParser
import com.github.worn.domain.model.AiAnalysisResult
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.TryItResult
import com.github.worn.domain.model.UserProfile
import com.github.worn.util.secret.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ClaudeApiClient(
    private val httpClient: HttpClient,
    private val secretStore: SecretStore,
) {
    // encodeDefaults: the image source's `type`/`media_type` are defaults the API requires.
    // explicitNulls: content blocks are a union, so unused members are omitted rather than nulled.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    suspend fun analyzeImage(imageBytes: ByteArray): AiAnalysisResult {
        val responseText = sendRequest(
            systemPrompt = AiPrompts.ANALYZE_SYSTEM_PROMPT,
            imageBytes = imageBytes,
            userText = "Analyze this clothing item image.",
        )
        return AiResponseParser.parseAnalysis(responseText)
    }

    suspend fun getGapRecommendations(
        items: List<ClothingItem>,
        userProfile: UserProfile? = null,
    ): List<GapRecommendation> {
        val profileContext = userProfile?.toPromptContext() ?: ""
        val responseText = sendRequest(
            systemPrompt = AiPrompts.GAPS_SYSTEM_PROMPT,
            userText = "${profileContext}My wardrobe:\n${AiPrompts.wardrobeSummary(items)}",
        )
        return AiResponseParser.parseGaps(responseText)
    }

    suspend fun analyzeProspectiveItem(
        imageBytes: ByteArray,
        existingItems: List<ClothingItem>,
        userProfile: UserProfile? = null,
    ): TryItResult {
        val wardrobeSummary = AiPrompts.wardrobeSummary(existingItems, includeIds = true)
        val profileContext = userProfile?.toPromptContext() ?: ""
        val responseText = sendRequest(
            systemPrompt = AiPrompts.TRY_IT_SYSTEM_PROMPT,
            imageBytes = imageBytes,
            userText = "${profileContext}Would this item fit my wardrobe?\n\nMy wardrobe:\n$wardrobeSummary",
        )
        return AiResponseParser.parseTryIt(responseText, existingItems)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun sendRequest(
        systemPrompt: String,
        userText: String,
        imageBytes: ByteArray? = null,
    ): String {
        val apiKey = secretStore.getApiKey()
            ?: error("Claude API key not configured. Add it in Settings.")

        val contentBlocks = buildList {
            if (imageBytes != null) {
                add(
                    ClaudeContent(
                        type = "image",
                        source = ClaudeImageSource(
                            data = Base64.encode(imageBytes),
                        ),
                    ),
                )
            }
            add(ClaudeContent(type = "text", text = userText))
        }

        val request = ClaudeRequest(
            model = MODEL,
            maxTokens = MAX_TOKENS,
            system = systemPrompt,
            messages = listOf(
                ClaudeMessage(role = "user", content = contentBlocks),
            ),
        )

        val response = try {
            httpClient.post(API_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", apiKey)
                header("anthropic-version", API_VERSION)
                setBody(json.encodeToString(ClaudeRequest.serializer(), request))
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            error("Network error. Check your connection.")
        }

        val responseBody = response.bodyAsText()

        if (!response.status.isSuccess()) {
            println("[$LOG_TAG] HTTP ${response.status.value} body=${responseBody.take(BODY_PREVIEW_LEN)}")
            val apiMessage = parseErrorMessage(responseBody)
            val userMessage = when (response.status.value) {
                // 400 covers billing ("credit balance is too low") and malformed requests alike.
                // Both are actionable and neither is fixed by retrying, so relay what the API said.
                HTTP_BAD_REQUEST -> apiMessage
                    ?: "Claude rejected the request (400). Please try again."
                HTTP_UNAUTHORIZED -> "Invalid API key. Check your key in Settings."
                HTTP_TOO_MANY_REQUESTS -> "Too many requests. Please wait and try again."
                in HTTP_SERVER_ERROR_RANGE -> "Claude service error. Please try again later."
                else -> "Unexpected error (${response.status.value}). Please try again."
            }
            error(userMessage)
        }

        val claudeResponse = json.decodeFromString<ClaudeResponse>(responseBody)
        return claudeResponse.content
            .firstOrNull { it.type == "text" }
            ?.text
            ?: error("No text content in Claude response")
    }

    /** Anthropic errors carry a human-readable reason; fall back to the status when absent. */
    private fun parseErrorMessage(body: String): String? =
        runCatching { json.decodeFromString<ClaudeErrorResponse>(body).error.message }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    companion object {
        private const val LOG_TAG = "ClaudeApi"
        private const val BODY_PREVIEW_LEN = 300
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val API_VERSION = "2023-06-01"
        private const val MODEL = "claude-sonnet-5"
        private const val MAX_TOKENS = 1024
        private const val HTTP_BAD_REQUEST = 400
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val HTTP_SERVER_ERROR_RANGE = 500..599
    }
}
