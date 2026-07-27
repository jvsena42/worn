package com.github.worn.data.source.ai

import com.github.worn.data.source.ai.AiPrompts.toPromptContext
import com.github.worn.domain.model.AiAnalysisResult
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.UserProfile

/**
 * The on-device counterpart to [ClaudeApiClient][com.github.worn.data.source.remote.ClaudeApiClient],
 * built on the same prompts and parser so both providers return identical domain models.
 *
 * Only the two features that a small local model handles well are exposed. Try-It analysis needs
 * to reason over the whole wardrobe at once and stays cloud-only, so it is absent here rather than
 * present-and-throwing — the type system keeps the repository honest.
 */
class OnDeviceAiSource(private val engine: OnDeviceAiEngine) {

    suspend fun analyzeImage(imageBytes: ByteArray): AiAnalysisResult {
        val responseText = engine.generate(
            systemPrompt = AiPrompts.ANALYZE_SYSTEM_PROMPT + AiPrompts.STRICT_JSON_SUFFIX,
            userText = "Analyze this clothing item image.",
            imageBytes = imageBytes,
        )
        return AiResponseParser.parseAnalysis(responseText)
    }

    suspend fun getGapRecommendations(
        items: List<ClothingItem>,
        userProfile: UserProfile? = null,
    ): List<GapRecommendation> {
        val profileContext = userProfile?.toPromptContext() ?: ""
        val responseText = engine.generate(
            systemPrompt = AiPrompts.GAPS_SYSTEM_PROMPT + AiPrompts.STRICT_JSON_SUFFIX,
            userText = "${profileContext}My wardrobe:\n${AiPrompts.wardrobeSummary(items)}",
        )
        return AiResponseParser.parseGaps(responseText)
    }
}
