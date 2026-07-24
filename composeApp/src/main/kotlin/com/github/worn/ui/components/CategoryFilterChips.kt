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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.ui.theme.WornColors

@Composable
fun CategoryFilterChips(
    activeCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allChips = listOf<Pair<Category?, String>>(null to stringResource(R.string.filter_all)) +
        Category.entries.map { it to it.displayName() }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(allChips, key = { it.first?.name ?: "all" }) { (category, label) ->
            WornChip(
                label = label,
                isActive = category == activeCategory,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun Category.displayName(): String = when (this) {
    Category.TOP -> stringResource(R.string.category_tops)
    Category.BOTTOM -> stringResource(R.string.category_bottoms)
    Category.OUTERWEAR -> stringResource(R.string.category_outerwear)
    Category.SHOES -> stringResource(R.string.category_shoes)
    Category.ACCESSORY -> stringResource(R.string.category_accessories)
}
