package com.github.worn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.ui.theme.WornColors
import java.io.File

private val photoShape = RoundedCornerShape(16.dp)

@Composable
fun ClothingCard(
    item: ClothingItem,
    photoHeight: Dp = 171.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhotoArea(item = item, height = photoHeight)
        ItemInfo(item = item)
    }
}

@Composable
private fun PhotoArea(item: ClothingItem, height: Dp) {
    Surface(
        shape = photoShape,
        color = WornColors.BgCard,
        border = BorderStroke(1.dp, WornColors.BorderSubtle),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
    ) {
        if (item.photoPath.isNotEmpty() && File(item.photoPath).exists()) {
            AsyncImage(
                model = File(item.photoPath),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(photoShape),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = null,
                    tint = WornColors.IconMuted,
                    modifier = Modifier.size(32.dp),
                )
            }
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
            color = WornColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
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
                color = WornColors.TextMuted,
                fontSize = 12.sp,
            )
        }
    }
}

private fun Category.dotColor(): Color = when (this) {
    Category.TOP -> Color(0xFF444444)
    Category.BOTTOM -> Color(0xFF2B4570)
    Category.DRESS -> Color(0xFFA87560)
    Category.OUTERWEAR -> Color(0xFF7A9468)
    Category.SHOES -> Color(0xFF8B6914)
    Category.ACCESSORY -> Color(0xFFB59D6E)
}

private fun Category.displayLabel(): String = when (this) {
    Category.TOP -> "Tops"
    Category.BOTTOM -> "Bottoms"
    Category.DRESS -> "Dresses"
    Category.OUTERWEAR -> "Outerwear"
    Category.SHOES -> "Shoes"
    Category.ACCESSORY -> "Accessories"
}
