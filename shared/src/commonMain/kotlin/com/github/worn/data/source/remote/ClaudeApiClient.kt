package com.github.worn.data.source.remote

import com.github.worn.domain.model.AiAnalysisResult
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.TryItResult
import com.github.worn.util.secret.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ClaudeApiClient(
    private val httpClient: HttpClient,
    private val secretStore: SecretStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyzeImage(imageBytes: ByteArray): AiAnalysisResult {
        val responseText = sendRequest(
            systemPrompt = ANALYZE_SYSTEM_PROMPT,
            imageBytes = imageBytes,
            userText = "Analyze this clothing item image.",
        )
        val parsed = json.decodeFromString<AiAnalysisJson>(responseText)
        return AiAnalysisResult(
            description = parsed.description,
            suggestedCategory = Category.valueOf(parsed.suggestedCategory.uppercase()),
            colors = parsed.colors,
            seasons = parsed.seasons.map { Season.valueOf(it.uppercase()) },
            tags = parsed.tags,
        )
    }

    suspend fun getGapRecommendations(
        items: List<ClothingItem>,
    ): List<GapRecommendation> {
        val wardrobeSummary = items.joinToString("\n") { item ->
            "- ${item.name} (${item.category}, colors: ${item.colors.joinToString()}, " +
                "seasons: ${item.seasons.joinToString()})"
        }
        val responseText = sendRequest(
            systemPrompt = GAPS_SYSTEM_PROMPT,
            userText = "My wardrobe:\n$wardrobeSummary",
        )
        val parsed = json.decodeFromString<List<GapRecommendationJson>>(responseText)
        return parsed.map {
            GapRecommendation(
                itemName = it.itemName,
                category = it.category,
                pairingCount = it.pairingCount,
            )
        }
    }

    suspend fun analyzeProspectiveItem(
        imageBytes: ByteArray,
        existingItems: List<ClothingItem>,
    ): TryItResult {
        val wardrobeSummary = existingItems.joinToString("\n") { item ->
            "- [${item.id}] ${item.name} (${item.category}, colors: ${item.colors.joinToString()}, " +
                "seasons: ${item.seasons.joinToString()})"
        }
        val responseText = sendRequest(
            systemPrompt = TRY_IT_SYSTEM_PROMPT,
            imageBytes = imageBytes,
            userText = "Would this item fit my wardrobe?\n\nMy wardrobe:\n$wardrobeSummary",
        )
        val parsed = json.decodeFromString<TryItResultJson>(responseText)
        val matchingItems = parsed.matchingItemIds.mapNotNull { id ->
            existingItems.find { it.id == id }
        }
        return TryItResult(
            matchingItems = matchingItems,
            combinationsUnlocked = parsed.combinationsUnlocked,
            gapsFilled = parsed.gapsFilled,
            worthAdding = parsed.worthAdding,
        )
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

        val response = httpClient.post(API_URL) {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", API_VERSION)
            setBody(json.encodeToString(ClaudeRequest.serializer(), request))
        }

        val claudeResponse = json.decodeFromString<ClaudeResponse>(response.bodyAsText())
        return claudeResponse.content
            .firstOrNull { it.type == "text" }
            ?.text
            ?: error("No text content in Claude response")
    }

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val API_VERSION = "2023-06-01"
        private const val MODEL = "claude-sonnet-4-20250514"
        private const val MAX_TOKENS = 1024

        private val ANALYZE_SYSTEM_PROMPT = """
            You are a fashion analysis AI. Analyze the clothing item in the image.
            Respond with ONLY a JSON object (no markdown):
            {
              "description": "brief description of the item",
              "suggested_category": "one of: TOP, BOTTOM, DRESS, OUTERWEAR, SHOES, ACCESSORY",
              "colors": ["color1", "color2"],
              "seasons": ["one or more of: SPRING, SUMMER, FALL, WINTER"],
              "tags": ["tag1", "tag2", "tag3"]
            }
        """.trimIndent()

        private val GAPS_SYSTEM_PROMPT = """
            You are a wardrobe analysis AI. Given a user's wardrobe, suggest items that would
            expand their outfit combinations the most. Group suggestions by category
            (BASICS, LAYERING, BOTTOMS, SHOES, ACCESSORIES).
            Respond with ONLY a JSON array (no markdown):
            [{"item_name": "...", "category": "...", "pairing_count": N}]
        """.trimIndent()

        private val TRY_IT_SYSTEM_PROMPT = """
            You are a wardrobe analysis AI. Given a photo of a prospective clothing item and
            the user's existing wardrobe, analyze how well this item would fit.
            Respond with ONLY a JSON object (no markdown):
            {
              "matching_item_ids": ["id1", "id2"],
              "combinations_unlocked": N,
              "gaps_filled": ["gap description 1", "gap description 2"],
              "worth_adding": true/false
            }
        """.trimIndent()
    }
}
