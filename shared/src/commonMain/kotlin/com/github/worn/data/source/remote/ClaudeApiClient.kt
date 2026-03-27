package com.github.worn.data.source.remote

import com.github.worn.domain.model.AiAnalysisResult
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
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
            suggestedCategory = runCatching {
                Category.valueOf(parsed.suggestedCategory.uppercase())
            }.getOrDefault(Category.TOP),
            colors = parsed.colors,
            seasons = parsed.seasons.mapNotNull {
                runCatching { Season.valueOf(it.uppercase()) }.getOrNull()
            },
            tags = parsed.tags,
            suggestedSubcategory = parsed.suggestedSubcategory?.let {
                runCatching { Subcategory.valueOf(it.uppercase()) }.getOrNull()
            },
            suggestedFit = parsed.suggestedFit?.let {
                runCatching { Fit.valueOf(it.uppercase()) }.getOrNull()
            },
            suggestedMaterial = parsed.suggestedMaterial?.let {
                runCatching { Material.valueOf(it.uppercase()) }.getOrNull()
            },
        )
    }

    suspend fun getGapRecommendations(
        items: List<ClothingItem>,
        userProfile: UserProfile? = null,
    ): List<GapRecommendation> {
        val wardrobeSummary = items.joinToString("\n") { item ->
            buildString {
                append("- ${item.name} (${item.category}")
                item.subcategory?.let { append(", type: $it") }
                append(", colors: ${item.colors.joinToString()}")
                append(", seasons: ${item.seasons.joinToString()}")
                item.fit?.let { append(", fit: $it") }
                item.material?.let { append(", material: $it") }
                append(")")
            }
        }
        val profileContext = userProfile?.toPromptContext() ?: ""
        val responseText = sendRequest(
            systemPrompt = GAPS_SYSTEM_PROMPT,
            userText = "${profileContext}My wardrobe:\n$wardrobeSummary",
        )
        val parsed = json.decodeFromString<List<GapRecommendationJson>>(responseText)
        return parsed.map { it.toDomain() }
    }

    suspend fun analyzeProspectiveItem(
        imageBytes: ByteArray,
        existingItems: List<ClothingItem>,
        userProfile: UserProfile? = null,
    ): TryItResult {
        val wardrobeSummary = existingItems.joinToString("\n") { item ->
            buildString {
                append("- [${item.id}] ${item.name} (${item.category}")
                item.subcategory?.let { append(", type: $it") }
                append(", colors: ${item.colors.joinToString()}")
                append(", seasons: ${item.seasons.joinToString()}")
                item.fit?.let { append(", fit: $it") }
                item.material?.let { append(", material: $it") }
                append(")")
            }
        }
        val profileContext = userProfile?.toPromptContext() ?: ""
        val responseText = sendRequest(
            systemPrompt = TRY_IT_SYSTEM_PROMPT,
            imageBytes = imageBytes,
            userText = "${profileContext}Would this item fit my wardrobe?\n\nMy wardrobe:\n$wardrobeSummary",
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
            You are a men's fashion analysis AI specialized in capsule wardrobe building.
            Analyze the clothing item in the image.
            Respond with ONLY a JSON object (no markdown):
            {
              "description": "brief description of the item",
              "suggested_category": "one of: TOP, BOTTOM, OUTERWEAR, SHOES, ACCESSORY",
              "colors": ["color1", "color2"],
              "seasons": ["one or more of: SPRING, SUMMER, FALL, WINTER"],
              "tags": ["tag1", "tag2", "tag3"],
              "suggested_subcategory": "one of: T_SHIRT, POLO, DRESS_SHIRT, HENLEY, SWEATER, HOODIE, JEANS, CHINOS, TAILORED_PANTS, SHORTS, CARGO_PANTS, SWEATPANTS, BOMBER, TRUCKER, PUFFER, BLAZER, COAT, WINDBREAKER, SNEAKERS, BOOTS_MILITARY, BOOTS_CHELSEA, DERBY, OXFORD, LOAFER, SANDALS, WATCH, BELT, SUNGLASSES, HAT_CAP, SCARF, BAG_BACKPACK",
              "suggested_fit": "one of: SLIM_FIT, REGULAR, RELAXED, OVERSIZED",
              "suggested_material": "one of: COTTON, LINEN, DENIM, WOOL, SYNTHETIC, LEATHER, SILK, KNIT"
            }
        """.trimIndent()

        private val GAPS_SYSTEM_PROMPT = """
            You are a men's capsule wardrobe analysis AI. Given a user's wardrobe, suggest
            versatile items that would maximize outfit combinations following capsule wardrobe
            principles. Prioritize timeless, mix-and-match pieces over trendy items.
            Group suggestions by category (BASICS, LAYERING, BOTTOMS, SHOES, ACCESSORIES).
            Respond with ONLY a JSON array (no markdown):
            [{"item_name": "...", "category": "...", "pairing_count": N,
              "subcategory": "one of: T_SHIRT, POLO, DRESS_SHIRT, HENLEY, SWEATER, HOODIE, JEANS, CHINOS, TAILORED_PANTS, SHORTS, CARGO_PANTS, SWEATPANTS, BOMBER, TRUCKER, PUFFER, BLAZER, COAT, WINDBREAKER, SNEAKERS, BOOTS_MILITARY, BOOTS_CHELSEA, DERBY, OXFORD, LOAFER, SANDALS, WATCH, BELT, SUNGLASSES, HAT_CAP, SCARF, BAG_BACKPACK",
              "colors": ["color1"],
              "seasons": ["SPRING", "SUMMER", "FALL", "WINTER"],
              "fit": "one of: SLIM_FIT, REGULAR, RELAXED, OVERSIZED",
              "material": "one of: COTTON, LINEN, DENIM, WOOL, SYNTHETIC, LEATHER, SILK, KNIT"}]
        """.trimIndent()

        private val TRY_IT_SYSTEM_PROMPT = """
            You are a men's capsule wardrobe analysis AI. Given a photo of a prospective
            clothing item and the user's existing wardrobe, evaluate how well this item
            contributes to versatility and outfit combinations following capsule wardrobe
            principles.
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

internal fun GapRecommendationJson.toDomain(): GapRecommendation = GapRecommendation(
    itemName = itemName,
    category = category,
    pairingCount = pairingCount,
    subcategory = subcategory?.let { runCatching { Subcategory.valueOf(it.uppercase()) }.getOrNull() },
    colors = colors,
    seasons = seasons.mapNotNull { runCatching { Season.valueOf(it.uppercase()) }.getOrNull() },
    fit = fit?.let { runCatching { Fit.valueOf(it.uppercase()) }.getOrNull() },
    material = material?.let { runCatching { Material.valueOf(it.uppercase()) }.getOrNull() },
    mappedCategory = mapDisplayCategoryToCategory(category),
)

private fun mapDisplayCategoryToCategory(displayCategory: String): Category =
    when (displayCategory.uppercase()) {
        "BASICS", "TOPS" -> Category.TOP
        "LAYERING" -> Category.OUTERWEAR
        "BOTTOMS" -> Category.BOTTOM
        "SHOES" -> Category.SHOES
        "ACCESSORIES" -> Category.ACCESSORY
        else -> Category.TOP
    }

private fun UserProfile.toPromptContext(): String {
    val parts = mutableListOf<String>()
    bodyType?.let { parts.add("Body type: ${it.name.lowercase().replace('_', ' ')}") }
    styleProfile?.let { parts.add("Style: ${it.name.lowercase().replace('_', ' ')}") }
    ageRange?.let {
        parts.add(
            "Age range: ${it.name.removePrefix("AGE_").replace('_', '-').replace("PLUS", "+")}",
        )
    }
    climate?.let { parts.add("Climate: ${it.name.lowercase()}") }
    if (lifestyles.isNotEmpty()) {
        parts.add("Lifestyle: ${lifestyles.joinToString { it.name.lowercase().replace('_', ' ') }}")
    }
    return if (parts.isEmpty()) "" else "User profile:\n${parts.joinToString("\n")}\n\n"
}
