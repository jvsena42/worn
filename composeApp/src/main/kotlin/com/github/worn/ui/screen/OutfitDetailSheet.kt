package com.github.worn.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Outfit
import com.github.worn.domain.model.Season
import com.github.worn.ui.components.ClothingPhoto
import com.github.worn.ui.components.PropertyRow
import com.github.worn.ui.components.SheetDragHandle
import com.github.worn.ui.exposeTestTagsAsResourceId
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.TabletPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitDetailSheet(
    outfit: Outfit,
    clothingItems: List<ClothingItem>,
    isCompact: Boolean,
    onEdit: (Outfit) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { SheetDragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
        OutfitDetailContent(
            outfit = outfit,
            clothingItems = clothingItems,
            isCompact = isCompact,
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }
}

@Composable
internal fun OutfitDetailContent(
    outfit: Outfit,
    clothingItems: List<ClothingItem>,
    isCompact: Boolean,
    onEdit: (Outfit) -> Unit = {},
    onDelete: (String) -> Unit = {},
) {
    val contentPadding = if (isCompact) 24.dp else 32.dp
    val sectionGap = if (isCompact) 20.dp else 24.dp
    val outfitItems = remember(outfit.itemIds, clothingItems) {
        outfit.itemIds.mapNotNull { id -> clothingItems.find { it.id == id } }
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .exposeTestTagsAsResourceId()
            .testTag("outfit_detail_sheet")
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(sectionGap),
    ) {
        OutfitTitle(
            name = outfit.name,
            nameStyle = if (isCompact) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.headlineSmall
            },
            padding = contentPadding,
        )
        OutfitItemsPreview(items = outfitItems, isCompact = isCompact, contentPadding = contentPadding)
        if (!isCompact) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = contentPadding)
                    .height(1.dp).background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
        OutfitProperties(outfit = outfit, items = outfitItems, isCompact = isCompact, padding = contentPadding)
        Box(modifier = Modifier.padding(horizontal = contentPadding)) {
            DetailActionButtons(
                editLabel = stringResource(R.string.outfit_detail_edit),
                deleteLabel = stringResource(R.string.outfit_detail_delete),
                buttonHeight = if (isCompact) 48.dp else 52.dp,
                buttonStyle = if (isCompact) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.titleSmall
                },
                onEdit = { onEdit(outfit) },
                onDelete = { showDeleteDialog = true },
                editTestTag = "outfit_detail_edit",
                deleteTestTag = "outfit_detail_delete",
            )
        }
    }

    if (showDeleteDialog) {
        DeleteOutfitDialog(
            outfitName = outfit.name,
            onConfirm = { showDeleteDialog = false; onDelete(outfit.id) },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun OutfitTitle(name: String, nameStyle: TextStyle, padding: Dp) {
    Text(
        text = name,
        color = MaterialTheme.colorScheme.onSurface,
        style = nameStyle,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = padding),
    )
}

@Composable
private fun OutfitItemsPreview(items: List<ClothingItem>, isCompact: Boolean, contentPadding: Dp) {
    val cardSize = if (isCompact) 200.dp else 300.dp
    val cardRadius = if (isCompact) 18.dp else 20.dp
    val cardGap = if (isCompact) 12.dp else 16.dp

    LazyRow(
        contentPadding = PaddingValues(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(cardGap),
    ) {
        items(items, key = { it.id }) { item ->
            OutfitItemCard(item = item, size = cardSize, cornerRadius = cardRadius)
        }
    }
}

@Composable
private fun OutfitProperties(outfit: Outfit, items: List<ClothingItem>, isCompact: Boolean, padding: Dp) {
    val propStyle = if (isCompact) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val propGap = if (isCompact) 14.dp else 16.dp

    Column(
        modifier = Modifier.padding(horizontal = padding),
        verticalArrangement = Arrangement.spacedBy(propGap),
    ) {
        PropertyRow(
            label = stringResource(R.string.label_items),
            value = stringResource(R.string.outfit_detail_items_count, outfit.itemIds.size),
            textStyle = propStyle,
        )
        PropertyRow(
            label = stringResource(R.string.label_season),
            value = deriveSeasonText(items),
            textStyle = propStyle,
        )
    }
}

@Composable
private fun DeleteOutfitDialog(outfitName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.outfit_detail_delete_dialog_title),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                stringResource(R.string.outfit_detail_delete_dialog_message, outfitName),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(24.dp),
            ) { Text(stringResource(R.string.common_delete), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun OutfitItemCard(
    item: ClothingItem,
    size: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
) {
    Column(
        modifier = Modifier.width(size),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 8.dp,
            modifier = Modifier.size(size),
        ) {
            ClothingPhoto(
                photoPath = item.photoPath,
                contentDescription = item.name,
                shape = RoundedCornerShape(cornerRadius),
                placeholderIconSize = 32.dp,
            )
        }
        Text(
            text = item.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}


@Composable
private fun deriveSeasonText(items: List<ClothingItem>): String {
    val context = LocalContext.current
    val allSeasons = items.flatMap { it.seasons }.toSet()
    return when {
        allSeasons.isEmpty() -> stringResource(R.string.common_not_specified)
        allSeasons.size == Season.entries.size -> stringResource(R.string.common_all_seasons)
        else -> allSeasons.joinToString("/") { season ->
            context.getString(season.stringRes())
        }
    }
}

@androidx.annotation.StringRes
private fun Season.stringRes(): Int = when (this) {
    Season.SPRING -> R.string.season_spring
    Season.SUMMER -> R.string.season_summer
    Season.FALL -> R.string.season_fall
    Season.WINTER -> R.string.season_winter
}

private val previewItems = listOf(
    ClothingItem("i1", "Black T-Shirt", Category.TOP, listOf("Black"), photoPath = "", createdAt = 0),
    ClothingItem("i2", "Navy Jeans", Category.BOTTOM, listOf("Navy"), photoPath = "", createdAt = 0),
    ClothingItem("i3", "White Sneakers", Category.SHOES, listOf("White"), photoPath = "", createdAt = 0),
    ClothingItem("i4", "Olive Jacket", Category.OUTERWEAR, listOf("Olive"), photoPath = "", createdAt = 0),
)

private val previewOutfit = Outfit(
    id = "1",
    name = "Weekend Casual",
    itemIds = listOf("i1", "i2", "i3", "i4"),
    createdAt = 1_710_460_800_000,
)

@PhonePreview
@Composable
private fun OutfitDetailSheetPhonePreview() {
    SheetPreview {
        OutfitDetailContent(
            outfit = previewOutfit,
            clothingItems = previewItems,
            isCompact = true,
        )
    }
}

@TabletPreview
@Composable
private fun OutfitDetailSheetTabletPreview() {
    SheetPreview {
        OutfitDetailContent(
            outfit = previewOutfit,
            clothingItems = previewItems,
            isCompact = false,
        )
    }
}

