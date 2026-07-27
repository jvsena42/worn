package com.github.worn.data.source.ai

import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.UserProfile

/**
 * Prompt text shared by every AI provider.
 *
 * Prompts live here rather than inside a client so the cloud ([ClaudeApiClient][
 * com.github.worn.data.source.remote.ClaudeApiClient]) and on-device ([OnDeviceAiSource])
 * providers ask for the exact same JSON contract, and [AiResponseParser] can parse either
 * reply with one code path.
 */
internal object AiPrompts {

    /**
     * On-device models are small and drift toward prose or fenced code even when told not to.
     * Appended to the prompts they serve; the Claude prompts are left untouched so existing
     * request-body assertions keep passing.
     */
    const val STRICT_JSON_SUFFIX = "\n\nOutput raw JSON only. No markdown, no code fences, " +
        "no explanation before or after."

    val ANALYZE_SYSTEM_PROMPT = """
        You are a men's fashion analysis AI specialized in capsule wardrobe building.
        Analyze the clothing item in the image.
        Respond with ONLY a JSON object (no markdown):
        {
          "description": "brief description of the item",
          "suggested_category": "one of: TOP, BOTTOM, OUTERWEAR, SHOES, ACCESSORY",
          "colors": ["color1", "color2"],
          "seasons": ["one or more of: SPRING, SUMMER, FALL, WINTER"],
          "tags": ["tag1", "tag2", "tag3"],
          "suggested_subcategory": "one of: $SUBCATEGORY_VALUES",
          "suggested_fit": "one of: SLIM_FIT, REGULAR, RELAXED, OVERSIZED",
          "suggested_material": "one of: COTTON, LINEN, DENIM, WOOL, SYNTHETIC, LEATHER, SILK, KNIT"
        }
    """.trimIndent()

    val GAPS_SYSTEM_PROMPT = """
        You are a men's capsule wardrobe analysis AI. Given a user's wardrobe, suggest
        versatile items that would maximize outfit combinations following capsule wardrobe
        principles. Prioritize timeless, mix-and-match pieces over trendy items.
        Group suggestions by category (BASICS, LAYERING, BOTTOMS, SHOES, ACCESSORIES).
        Respond with ONLY a JSON array (no markdown):
        [{"item_name": "...", "category": "...", "pairing_count": N,
          "subcategory": "one of: $SUBCATEGORY_VALUES",
          "colors": ["color1"],
          "seasons": ["SPRING", "SUMMER", "FALL", "WINTER"],
          "fit": "one of: SLIM_FIT, REGULAR, RELAXED, OVERSIZED",
          "material": "one of: COTTON, LINEN, DENIM, WOOL, SYNTHETIC, LEATHER, SILK, KNIT"}]
    """.trimIndent()

    val TRY_IT_SYSTEM_PROMPT = """
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

    /**
     * One line per item. [includeIds] prefixes each line with the item id, which only the
     * try-it prompt needs — it asks the model to echo ids back in `matching_item_ids`.
     */
    fun wardrobeSummary(items: List<ClothingItem>, includeIds: Boolean = false): String =
        items.joinToString("\n") { item ->
            buildString {
                append("- ")
                if (includeIds) append("[${item.id}] ")
                append("${item.name} (${item.category}")
                item.subcategory?.let { append(", type: $it") }
                append(", colors: ${item.colors.joinToString()}")
                append(", seasons: ${item.seasons.joinToString()}")
                item.fit?.let { append(", fit: $it") }
                item.material?.let { append(", material: $it") }
                append(")")
            }
        }

    fun UserProfile.toPromptContext(): String {
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
}

private const val SUBCATEGORY_VALUES = "T_SHIRT, POLO, DRESS_SHIRT, HENLEY, SWEATER, HOODIE, " +
    "JEANS, CHINOS, TAILORED_PANTS, SHORTS, CARGO_PANTS, SWEATPANTS, BOMBER, TRUCKER, PUFFER, " +
    "BLAZER, COAT, WINDBREAKER, SNEAKERS, BOOTS_MILITARY, BOOTS_CHELSEA, DERBY, OXFORD, LOAFER, " +
    "SANDALS, WATCH, BELT, SUNGLASSES, HAT_CAP, SCARF, BAG_BACKPACK"
