package com.github.worn.domain.model

data class AiAnalysisResult(
    val description: String,
    val suggestedCategory: Category,
    val colors: List<String>,
    val seasons: List<Season>,
    val tags: List<String>,
)
