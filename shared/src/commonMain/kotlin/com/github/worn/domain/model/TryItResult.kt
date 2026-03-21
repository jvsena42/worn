package com.github.worn.domain.model

data class TryItResult(
    val matchingItems: List<ClothingItem>,
    val combinationsUnlocked: Int,
    val gapsFilled: List<String>,
    val worthAdding: Boolean,
)
