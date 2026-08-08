@file:Suppress("TooManyFunctions")

package com.github.worn.ui.components

import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.domain.model.subcategoriesFor
import com.github.worn.ui.theme.wornExtras

val addItemColorPalette = listOf(
    "White" to Color(0xFFFFFFFF),
    "Cream" to Color(0xFFEDE8E1),
    "Black" to Color(0xFF2C2924),
    "Navy" to Color(0xFF2B4570),
    "Grey" to Color(0xFF808080),
    "Charcoal" to Color(0xFF36454F),
    "Olive" to Color(0xFF6B7B3F),
    "Beige" to Color(0xFFC4A882),
    "Khaki" to Color(0xFFC3B091),
    "Tan" to Color(0xFFD2B48C),
    "Brown" to Color(0xFF8B4513),
    "Burgundy" to Color(0xFF800020),
    "Coral" to Color(0xFFA87560),
    "Light Blue" to Color(0xFFADD8E6),
)

@Composable
fun PhotoUploadZone(
    bitmap: ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth().height(140.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "Selected photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
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
                        tint = MaterialTheme.wornExtras.iconMuted,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.add_item_photo_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (isProcessing) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun RemoveBackgroundToggle(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.add_item_remove_background),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
fun AiBadge(onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier,
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text("✦ ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.add_item_ai_badge),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun ItemNameField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                stringResource(R.string.add_item_name_hint),
                color = MaterialTheme.wornExtras.iconMuted,
                fontSize = 15.sp,
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
    )
}

@Composable
fun CategoryDropdown(selected: Category?, onSelected: (Category) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(12.dp))
                    }
                    Text(
                        text = selected?.displayName() ?: stringResource(R.string.label_category),
                        color = if (selected != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.wornExtras.iconMuted
                        },
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
                        else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.wornExtras.iconMuted,
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = category.displayName(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (category != Category.entries.last()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSection(selectedColors: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.label_color),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            addItemColorPalette.forEach { (name, color) ->
                val isSelected = name in selectedColors
                Surface(
                    onClick = { onToggle(name) },
                    shape = CircleShape,
                    color = color,
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
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
        Text(
            stringResource(R.string.label_season),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Season.entries.forEach { season ->
                WornChip(
                    label = season.displayName(),
                    isActive = season in selectedSeasons,
                    onClick = { onToggle(season) },
                )
            }
        }
    }
}

@Composable
fun SaveButton(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    WornGradientButton(
        text = if (isSaving) {
            stringResource(R.string.common_saving)
        } else {
            label ?: stringResource(R.string.add_item_save_to_wardrobe)
        },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@DrawableRes
internal fun Category.iconRes(): Int = when (this) {
    Category.TOP -> R.drawable.ic_shirt
    Category.BOTTOM -> R.drawable.ic_rectangle_horizontal
    Category.OUTERWEAR -> R.drawable.ic_wind
    Category.SHOES -> R.drawable.ic_footprints
    Category.ACCESSORY -> R.drawable.ic_glasses
}

@Composable
private fun Category.displayName(): String = when (this) {
    Category.TOP -> stringResource(R.string.category_tops)
    Category.BOTTOM -> stringResource(R.string.category_bottoms)
    Category.OUTERWEAR -> stringResource(R.string.category_outerwear)
    Category.SHOES -> stringResource(R.string.category_shoes)
    Category.ACCESSORY -> stringResource(R.string.category_accessories)
}

@Composable
fun SubcategoryDropdown(category: Category, selected: Subcategory?, onSelected: (Subcategory) -> Unit) {
    val options = subcategoriesFor(category)
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Surface(
                onClick = { expanded = !expanded },
                color = Color.Transparent,
            ) {
                DropdownHeader(
                    text = selected?.displayName() ?: stringResource(R.string.label_subcategory),
                    hasSelection = selected != null,
                    expanded = expanded,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                SubcategoryOptionList(options = options, onSelected = {
                    onSelected(it)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun DropdownHeader(text: String, hasSelection: Boolean, expanded: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Text(
            text = text,
            color = if (hasSelection) MaterialTheme.colorScheme.onSurface else MaterialTheme.wornExtras.iconMuted,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
            else Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.wornExtras.iconMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SubcategoryOptionList(options: List<Subcategory>, onSelected: (Subcategory) -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        options.forEach { subcategory ->
            Surface(onClick = { onSelected(subcategory) }, color = Color.Transparent) {
                Text(
                    text = subcategory.displayName(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            if (subcategory != options.last()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FitSection(selected: Fit?, onSelected: (Fit?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.label_fit),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Fit.entries.forEach { fit ->
                WornChip(
                    label = fit.displayName(),
                    isActive = fit == selected,
                    onClick = { onSelected(if (fit == selected) null else fit) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialSection(selected: Material?, onSelected: (Material?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.label_material),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Material.entries.forEach { material ->
                WornChip(
                    label = material.displayName(),
                    isActive = material == selected,
                    onClick = { onSelected(if (material == selected) null else material) },
                )
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun Subcategory.displayName(): String = stringResource(
    when (this) {
        Subcategory.T_SHIRT -> R.string.subcategory_t_shirt
        Subcategory.POLO -> R.string.subcategory_polo
        Subcategory.DRESS_SHIRT -> R.string.subcategory_dress_shirt
        Subcategory.LONG_SLEEVE_SHIRT -> R.string.subcategory_long_sleeve_shirt
        Subcategory.HENLEY -> R.string.subcategory_henley
        Subcategory.JEANS -> R.string.subcategory_jeans
        Subcategory.CHINOS -> R.string.subcategory_chinos
        Subcategory.TAILORED_PANTS -> R.string.subcategory_tailored_pants
        Subcategory.SHORTS -> R.string.subcategory_shorts
        Subcategory.CARGO_PANTS -> R.string.subcategory_cargo_pants
        Subcategory.SWEATPANTS -> R.string.subcategory_sweatpants
        Subcategory.SWEATER -> R.string.subcategory_sweater
        Subcategory.HOODIE -> R.string.subcategory_hoodie
        Subcategory.BOMBER -> R.string.subcategory_bomber
        Subcategory.TRUCKER -> R.string.subcategory_trucker
        Subcategory.PUFFER -> R.string.subcategory_puffer
        Subcategory.BLAZER -> R.string.subcategory_blazer
        Subcategory.COAT -> R.string.subcategory_coat
        Subcategory.WINDBREAKER -> R.string.subcategory_windbreaker
        Subcategory.SNEAKERS -> R.string.subcategory_sneakers
        Subcategory.BOOTS_MILITARY -> R.string.subcategory_boots_military
        Subcategory.BOOTS_CHELSEA -> R.string.subcategory_boots_chelsea
        Subcategory.DERBY -> R.string.subcategory_derby
        Subcategory.OXFORD -> R.string.subcategory_oxford
        Subcategory.LOAFER -> R.string.subcategory_loafer
        Subcategory.SANDALS -> R.string.subcategory_sandals
        Subcategory.WATCH -> R.string.subcategory_watch
        Subcategory.BELT -> R.string.subcategory_belt
        Subcategory.SUNGLASSES -> R.string.subcategory_sunglasses
        Subcategory.HAT_CAP -> R.string.subcategory_hat_cap
        Subcategory.SCARF -> R.string.subcategory_scarf
        Subcategory.BAG_BACKPACK -> R.string.subcategory_bag_backpack
    },
)

@Composable
internal fun Fit.displayName(): String = stringResource(
    when (this) {
        Fit.SLIM_FIT -> R.string.fit_slim
        Fit.REGULAR -> R.string.fit_regular
        Fit.RELAXED -> R.string.fit_relaxed
        Fit.OVERSIZED -> R.string.fit_oversized
    },
)

@Composable
internal fun Material.displayName(): String = stringResource(
    when (this) {
        Material.COTTON -> R.string.material_cotton
        Material.LINEN -> R.string.material_linen
        Material.DENIM -> R.string.material_denim
        Material.WOOL -> R.string.material_wool
        Material.SYNTHETIC -> R.string.material_synthetic
        Material.LEATHER -> R.string.material_leather
        Material.SILK -> R.string.material_silk
        Material.KNIT -> R.string.material_knit
    },
)

@Composable
internal fun Season.displayName(): String = stringResource(
    when (this) {
        Season.SPRING -> R.string.season_spring
        Season.SUMMER -> R.string.season_summer
        Season.FALL -> R.string.season_fall
        Season.WINTER -> R.string.season_winter
    },
)

@Composable
internal fun Category.displayLabel(): String = when (this) {
    Category.TOP -> stringResource(R.string.category_tops)
    Category.BOTTOM -> stringResource(R.string.category_bottoms)
    Category.OUTERWEAR -> stringResource(R.string.category_outerwear)
    Category.SHOES -> stringResource(R.string.category_shoes)
    Category.ACCESSORY -> stringResource(R.string.category_accessories)
}

