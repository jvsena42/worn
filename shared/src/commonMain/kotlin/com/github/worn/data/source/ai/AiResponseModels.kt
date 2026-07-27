package com.github.worn.data.source.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The JSON contract [AiPrompts] asks every provider for. Provider-neutral on purpose: these are
 * shapes *we* define in the prompt, unlike the transport models in `ClaudeApiModels.kt` which
 * are dictated by the Anthropic API.
 */
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
