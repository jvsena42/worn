package com.github.worn.fake

import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Season

fun clothingItem(
    id: String = "item-1",
    name: String = "Blue T-Shirt",
    category: Category = Category.TOP,
    colors: List<String> = listOf("blue"),
    seasons: List<Season> = listOf(Season.SUMMER),
    tags: List<String> = emptyList(),
    description: String? = null,
    photoPath: String = "/photos/$id.jpg",
    createdAt: Long = 1_000_000L,
): ClothingItem = ClothingItem(
    id = id,
    name = name,
    category = category,
    colors = colors,
    seasons = seasons,
    tags = tags,
    description = description,
    photoPath = photoPath,
    createdAt = createdAt,
)
