package com.github.worn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.Outfit
import com.github.worn.ui.theme.wornExtras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val cardShape = RoundedCornerShape(20.dp)

private val thumbnailShape = RoundedCornerShape(10.dp)
private val badgeShape = RoundedCornerShape(8.dp)

private val badgeColors: List<Color>
    @Composable @ReadOnlyComposable
    get() = listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutfitCard(
    outfit: Outfit,
    itemCategories: Map<String, Category> = emptyMap(),
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        Row(modifier = Modifier.padding(20.dp)) {
            if (isSelectionMode) {
                SelectionIndicator(isSelected = isSelected)
                Spacer(Modifier.size(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                ItemThumbnailRow(outfit = outfit, itemCategories = itemCategories)
                Spacer(Modifier.height(12.dp))
                BottomRow(outfit = outfit)
            }
        }
    }
}

@Composable
private fun ItemThumbnailRow(
    outfit: Outfit,
    itemCategories: Map<String, Category>,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val displayIds = outfit.itemIds.take(4)
        displayIds.forEach { itemId ->
            ItemThumbnail(category = itemCategories[itemId])
        }
        Spacer(Modifier.weight(1f))
        ItemCountBadge(outfit = outfit)
    }
}

@Composable
private fun ItemThumbnail(category: Category?) {
    Surface(
        shape = thumbnailShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(id = (category ?: Category.TOP).iconRes()),
                contentDescription = null,
                tint = MaterialTheme.wornExtras.iconMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ItemCountBadge(outfit: Outfit) {
    val badgeColor = badgeColors[outfit.id.hashCode().mod(badgeColors.size)]
    Surface(shape = badgeShape, color = badgeColor) {
        Text(
            text = stringResource(R.string.outfit_detail_items_count, outfit.itemIds.size),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun BottomRow(outfit: Outfit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = outfit.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                // Auto-generated names concatenate every item, so they can outgrow the card.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDate(outfit.createdAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.wornExtras.iconMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}


// Shared rather than allocated per card per recomposition. SimpleDateFormat is not thread-safe,
// but composition is single-threaded.
private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

private fun formatDate(epochMillis: Long): String {
    return dateFormat.format(Date(epochMillis))
}
