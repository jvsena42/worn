package com.github.worn.domain.model

data class ClothingItem(
    val id: String,
    val name: String,
    val category: Category,
    val colors: List<String>,
    val seasons: List<Season> = emptyList(),
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val subcategory: Subcategory? = null,
    val fit: Fit? = null,
    val material: Material? = null,
    val photoPath: String,
    val createdAt: Long,
)
