package com.github.worn.data.source.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ClaudeMessage>,
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
internal data class AiAnalysisJson(
    val description: String,
    @SerialName("suggested_category") val suggestedCategory: String,
    val colors: List<String>,
    val seasons: List<String>,
    val tags: List<String>,
    @SerialName("suggested_subcategory") val suggestedSubcategory: String? = null,
    @SerialName("suggested_fit") val suggestedFit: String? = null,
    @SerialName("suggested_material") val suggestedMaterial: String? = null,
)

@Serializable
internal data class GapRecommendationJson(
    @SerialName("item_name") val itemName: String,
    val category: String,
    @SerialName("pairing_count") val pairingCount: Int,
    val subcategory: String? = null,
    val colors: List<String> = emptyList(),
    val seasons: List<String> = emptyList(),
    val fit: String? = null,
    val material: String? = null,
)

@Serializable
internal data class TryItResultJson(
    @SerialName("matching_item_ids") val matchingItemIds: List<String>,
    @SerialName("combinations_unlocked") val combinationsUnlocked: Int,
    @SerialName("gaps_filled") val gapsFilled: List<String>,
    @SerialName("worth_adding") val worthAdding: Boolean,
)
