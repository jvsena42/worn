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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.Outfit
import com.github.worn.ui.theme.WornColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val cardShape = RoundedCornerShape(20.dp)
private val selectionShape = RoundedCornerShape(14.dp)
private val thumbnailShape = RoundedCornerShape(10.dp)
private val badgeShape = RoundedCornerShape(8.dp)

private val badgeColors = listOf(
    Color(0xFF6B7B8E),
    Color(0xFFA87560),
    Color(0xFF7A9468),
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
        color = WornColors.BgCard,
        border = BorderStroke(
            1.dp,
            if (isSelected) WornColors.AccentGreen else WornColors.BorderSubtle,
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
        color = WornColors.BgElevated,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(id = (category ?: Category.TOP).iconRes()),
                contentDescription = null,
                tint = WornColors.IconMuted,
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
            text = "${outfit.itemIds.size} items",
            color = WornColors.TextOnColor,
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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = outfit.name,
                color = WornColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatDate(outfit.createdAt),
                color = WornColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = WornColors.IconMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SelectionIndicator(isSelected: Boolean) {
    Surface(
        shape = selectionShape,
        color = if (isSelected) WornColors.AccentGreen else WornColors.BgCard,
        border = if (isSelected) null else BorderStroke(1.5.dp, WornColors.BorderSubtle),
        modifier = Modifier.size(28.dp),
    ) {
        if (isSelected) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}
