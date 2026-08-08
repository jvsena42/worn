package com.github.worn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.ui.theme.wornExtras

private val photoShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClothingCard(
    item: ClothingItem,
    photoHeight: Dp = 171.dp,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongPress,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhotoArea(
            item = item,
            height = photoHeight,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
        )
        ItemInfo(item = item)
    }
}

@Composable
private fun PhotoArea(
    item: ClothingItem,
    height: Dp,
    isSelected: Boolean,
    isSelectionMode: Boolean,
) {
    Box(modifier = Modifier.fillMaxWidth().height(height)) {
        Surface(
            shape = photoShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            ClothingPhoto(
                photoPath = item.photoPath,
                contentDescription = item.name,
                shape = photoShape,
                placeholderIconSize = 32.dp,
            )
        }

        if (isSelectionMode) {
            SelectionIndicator(
                isSelected = isSelected,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}


@Composable
private fun ItemInfo(item: ClothingItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = item.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            // AI-generated names can run long; left unbounded they push the category row down and
            // misalign the cards next to them in the grid row.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(item.category.dotColor()),
            )
            Text(
                text = item.category.displayLabel(),
                color = MaterialTheme.wornExtras.textMuted,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
@ReadOnlyComposable
internal fun Category.dotColor(): Color = when (this) {
    Category.TOP -> MaterialTheme.wornExtras.categoryDotTop
    Category.BOTTOM -> MaterialTheme.wornExtras.categoryDotBottom
    Category.OUTERWEAR -> MaterialTheme.wornExtras.categoryDotOuterwear
    Category.SHOES -> MaterialTheme.wornExtras.categoryDotShoes
    Category.ACCESSORY -> MaterialTheme.wornExtras.categoryDotAccessory
}
