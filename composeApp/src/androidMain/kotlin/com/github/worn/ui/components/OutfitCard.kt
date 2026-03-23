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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.domain.model.Outfit
import com.github.worn.ui.theme.WornColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val cardShape = RoundedCornerShape(16.dp)
private val selectionShape = RoundedCornerShape(14.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutfitCard(
    outfit: Outfit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = cardShape,
        color = WornColors.BgCard,
        border = BorderStroke(1.dp, if (isSelected) WornColors.AccentGreen else WornColors.BorderSubtle),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                SelectionIndicator(isSelected = isSelected)
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = if (isSelectionMode) 12.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = outfit.name,
                    color = WornColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${outfit.itemIds.size} item${if (outfit.itemIds.size != 1) "s" else ""}",
                        color = WornColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "·",
                        color = WornColors.TextMuted,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = formatDate(outfit.createdAt),
                        color = WornColors.TextMuted,
                        fontSize = 13.sp,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = WornColors.TextMuted,
                modifier = Modifier.size(24.dp),
            )
        }
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
