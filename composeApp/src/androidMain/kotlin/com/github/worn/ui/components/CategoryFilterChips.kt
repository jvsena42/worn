package com.github.worn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.domain.model.Category
import com.github.worn.ui.theme.WornColors

private val chipShape = RoundedCornerShape(20.dp)

@Composable
fun CategoryFilterChips(
    activeCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allChips = listOf<Pair<Category?, String>>(null to "All") +
        Category.entries.map { it to it.displayName() }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(allChips, key = { it.first?.name ?: "all" }) { (category, label) ->
            val isActive = category == activeCategory
            CategoryChip(
                label = label,
                isActive = isActive,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = chipShape,
        color = if (isActive) WornColors.AccentGreen else WornColors.BgCard,
        border = if (isActive) null else BorderStroke(1.dp, WornColors.BorderSubtle),
    ) {
        Text(
            text = label,
            color = if (isActive) WornColors.TextOnColor else WornColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

private fun Category.displayName(): String = when (this) {
    Category.TOP -> "Tops"
    Category.BOTTOM -> "Bottoms"
    Category.OUTERWEAR -> "Outerwear"
    Category.SHOES -> "Shoes"
    Category.ACCESSORY -> "Accessories"
}
