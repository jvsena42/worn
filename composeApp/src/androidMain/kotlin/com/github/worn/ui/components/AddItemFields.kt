@file:Suppress("TooManyFunctions")

package com.github.worn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.Season
import com.github.worn.ui.theme.WornColors

val addItemColorPalette = listOf(
    "Cream" to Color(0xFFEDE8E1),
    "Black" to Color(0xFF2C2924),
    "Navy" to Color(0xFF2B4570),
    "Grey" to Color(0xFF808080),
    "Olive" to Color(0xFF6B7B3F),
    "Beige" to Color(0xFFC4A882),
    "Brown" to Color(0xFF8B4513),
    "Coral" to Color(0xFFA87560),
)

@Composable
fun PhotoUploadZone(bitmap: ImageBitmap?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, WornColors.BorderStrong),
        modifier = Modifier.fillMaxWidth().height(140.dp),
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "Selected photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = WornColors.IconMuted,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap to add photo",
                    color = WornColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun AiBadge(onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF6B7B8E),
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text("✦ ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Auto-tag with AI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ItemNameField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Item name", color = WornColors.IconMuted, fontSize = 15.sp) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = WornColors.BgCard,
            unfocusedContainerColor = WornColors.BgCard,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, WornColors.BorderSubtle, RoundedCornerShape(12.dp)),
    )
}

@Composable
fun CategoryDropdown(selected: Category?, onSelected: (Category) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WornColors.BgCard,
        border = BorderStroke(1.dp, WornColors.BorderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Surface(
                onClick = { expanded = !expanded },
                color = Color.Transparent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    if (selected != null) {
                        Icon(
                            painter = painterResource(selected.iconRes()),
                            contentDescription = null,
                            tint = WornColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(12.dp))
                    }
                    Text(
                        text = selected?.displayName() ?: "Category",
                        color = if (selected != null) WornColors.TextPrimary else WornColors.IconMuted,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
                        else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = WornColors.IconMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                CategoryOptionList(onSelected = {
                    onSelected(it)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun CategoryOptionList(onSelected: (Category) -> Unit) {
    Column {
        HorizontalDivider(color = WornColors.BorderSubtle)
        Category.entries.forEach { category ->
            Surface(
                onClick = { onSelected(category) },
                color = Color.Transparent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        painter = painterResource(category.iconRes()),
                        contentDescription = null,
                        tint = WornColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = category.displayName(),
                        color = WornColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (category != Category.entries.last()) {
                HorizontalDivider(color = WornColors.BorderSubtle.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSection(selectedColors: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Color", color = WornColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            addItemColorPalette.forEach { (name, color) ->
                val isSelected = name in selectedColors
                Surface(
                    onClick = { onToggle(name) },
                    shape = CircleShape,
                    color = color,
                    border = if (isSelected) BorderStroke(2.dp, WornColors.AccentGreen) else null,
                    modifier = Modifier.size(28.dp),
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = contrastColor(color),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun contrastColor(background: Color): Color {
    val brightness = background.red * 0.299f + background.green * 0.587f + background.blue * 0.114f
    return if (brightness > 0.5f) Color.Black else Color.White
}

@Composable
fun SeasonSection(selectedSeasons: Set<Season>, onToggle: (Season) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Season", color = WornColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Season.entries.forEach { season ->
                val isActive = season in selectedSeasons
                Surface(
                    onClick = { onToggle(season) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isActive) WornColors.AccentGreen else WornColors.BgCard,
                    border = if (isActive) null else BorderStroke(1.dp, WornColors.BorderSubtle),
                ) {
                    Text(
                        text = season.displayName(),
                        color = if (isActive) WornColors.TextOnColor else WornColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SaveButton(enabled: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF8FA47D), Color(0xFF6B7F5E)))
    val disabledGradient = Brush.verticalGradient(listOf(Color(0xFFB5AFA8), Color(0xFFA09A92)))
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(if (enabled) gradient else disabledGradient),
        ) {
            Text(
                text = if (isSaving) "Saving…" else "Save to wardrobe",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@DrawableRes
private fun Category.iconRes(): Int = when (this) {
    Category.TOP -> R.drawable.ic_shirt
    Category.BOTTOM -> R.drawable.ic_rectangle_horizontal
    Category.DRESS -> R.drawable.ic_dress
    Category.OUTERWEAR -> R.drawable.ic_wind
    Category.SHOES -> R.drawable.ic_footprints
    Category.ACCESSORY -> R.drawable.ic_glasses
}

private fun Category.displayName(): String = when (this) {
    Category.TOP -> "Tops"
    Category.BOTTOM -> "Bottoms"
    Category.DRESS -> "Dresses"
    Category.OUTERWEAR -> "Outerwear"
    Category.SHOES -> "Shoes"
    Category.ACCESSORY -> "Accessories"
}

private fun Season.displayName(): String = when (this) {
    Season.SPRING -> "Spring"
    Season.SUMMER -> "Summer"
    Season.FALL -> "Fall"
    Season.WINTER -> "Winter"
}
