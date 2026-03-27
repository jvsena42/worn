package com.github.worn.domain.model

data class GapRecommendation(
    val itemName: String,
    val category: String,
    val pairingCount: Int,
    val subcategory: Subcategory? = null,
    val colors: List<String> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val fit: Fit? = null,
    val material: Material? = null,
    val mappedCategory: Category = Category.TOP,
)
