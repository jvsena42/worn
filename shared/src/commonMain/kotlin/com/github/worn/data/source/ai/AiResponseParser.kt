package com.github.worn.data.source.ai

import com.github.worn.domain.model.AiAnalysisResult
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.TryItResult
import kotlinx.serialization.json.Json

/**
 * Turns a model's raw reply into domain models, for the cloud and on-device providers alike.
 *
 * Enum values are parsed leniently — a model that invents a category shouldn't fail the whole
 * analysis, so unknown values become `null` (or the [Category.TOP] default) rather than throwing.
 */
internal object AiResponseParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseAnalysis(responseText: String): AiAnalysisResult {
        val parsed = json.decodeFromString<AiAnalysisJson>(stripCodeFence(responseText))
        return AiAnalysisResult(
            description = parsed.description,
            suggestedCategory = parsed.suggestedCategory.toEnumOrNull<Category>() ?: Category.TOP,
            colors = parsed.colors,
            seasons = parsed.seasons.mapNotNull { it.toEnumOrNull<Season>() },
            tags = parsed.tags,
            suggestedSubcategory = parsed.suggestedSubcategory?.toEnumOrNull<Subcategory>(),
            suggestedFit = parsed.suggestedFit?.toEnumOrNull<Fit>(),
            suggestedMaterial = parsed.suggestedMaterial?.toEnumOrNull<Material>(),
        )
    }

    fun parseGaps(responseText: String): List<GapRecommendation> =
        json.decodeFromString<List<GapRecommendationJson>>(stripCodeFence(responseText))
            .map { it.toDomain() }

    fun parseTryIt(responseText: String, existingItems: List<ClothingItem>): TryItResult {
        val parsed = json.decodeFromString<TryItResultJson>(stripCodeFence(responseText))
        return TryItResult(
            matchingItems = parsed.matchingItemIds.mapNotNull { id ->
                existingItems.find { it.id == id }
            },
            combinationsUnlocked = parsed.combinationsUnlocked,
            gapsFilled = parsed.gapsFilled,
            worthAdding = parsed.worthAdding,
        )
    }

    /**
     * On-device models wrap JSON in a ```json fence despite being told not to. Unwrap it rather
     * than failing the request; already-bare JSON passes through untouched.
     */
    fun stripCodeFence(responseText: String): String {
        val trimmed = responseText.trim()
        if (!trimmed.startsWith("```")) return trimmed
        return trimmed
            .removePrefix("```")
            .substringAfter('\n', missingDelimiterValue = "")
            .substringBeforeLast("```")
            .trim()
    }
}

internal fun GapRecommendationJson.toDomain(): GapRecommendation = GapRecommendation(
    itemName = itemName,
    category = category,
    pairingCount = pairingCount,
    subcategory = subcategory?.toEnumOrNull<Subcategory>(),
    colors = colors,
    seasons = seasons.mapNotNull { it.toEnumOrNull<Season>() },
    fit = fit?.toEnumOrNull<Fit>(),
    material = material?.toEnumOrNull<Material>(),
    mappedCategory = mapDisplayCategoryToCategory(category),
)

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    runCatching { enumValueOf<T>(trim().uppercase()) }.getOrNull()

private fun mapDisplayCategoryToCategory(displayCategory: String): Category =
    when (displayCategory.uppercase()) {
        "BASICS", "TOPS" -> Category.TOP
        "LAYERING" -> Category.OUTERWEAR
        "BOTTOMS" -> Category.BOTTOM
        "SHOES" -> Category.SHOES
        "ACCESSORIES" -> Category.ACCESSORY
        else -> Category.TOP
    }
