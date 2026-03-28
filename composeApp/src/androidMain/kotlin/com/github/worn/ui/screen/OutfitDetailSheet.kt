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
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Outfit
import com.github.worn.domain.model.Season
import com.github.worn.ui.components.displayName
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.WornColors
import java.io.File

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
        containerColor = WornColors.BgElevated,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { OutfitSheetDragHandle() },
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
private fun OutfitSheetDragHandle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(WornColors.BorderStrong),
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
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(sectionGap),
    ) {
        OutfitTitle(name = outfit.name, nameSize = if (isCompact) 22.sp else 26.sp, padding = contentPadding)
        OutfitItemsPreview(items = outfitItems, isCompact = isCompact, contentPadding = contentPadding)
        if (!isCompact) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = contentPadding)
                    .height(1.dp).background(WornColors.BorderSubtle),
            )
        }
        OutfitProperties(outfit = outfit, items = outfitItems, isCompact = isCompact, padding = contentPadding)
        Box(modifier = Modifier.padding(horizontal = contentPadding)) {
            DetailActionButtons(
                editLabel = "Edit Outfit",
                deleteLabel = "Delete Outfit",
                buttonHeight = if (isCompact) 48.dp else 52.dp,
                buttonFontSize = if (isCompact) 15.sp else 16.sp,
                onEdit = { onEdit(outfit) },
                onDelete = { showDeleteDialog = true },
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
private fun OutfitTitle(name: String, nameSize: TextUnit, padding: Dp) {
    Text(
        text = name,
        color = WornColors.TextPrimary,
        fontSize = nameSize,
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
    val propFontSize = if (isCompact) 14.sp else 15.sp
    val propGap = if (isCompact) 14.dp else 16.dp

    Column(
        modifier = Modifier.padding(horizontal = padding),
        verticalArrangement = Arrangement.spacedBy(propGap),
    ) {
        OutfitPropertyRow(label = "Items", value = "${outfit.itemIds.size} items", fontSize = propFontSize)
        OutfitPropertyRow(label = "Season", value = deriveSeasonText(items), fontSize = propFontSize)
    }
}

@Composable
private fun DeleteOutfitDialog(outfitName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete outfit?", fontWeight = FontWeight.SemiBold, fontSize = 22.sp) },
        text = {
            Text(
                "This action cannot be undone. \"$outfitName\" will be permanently removed.",
                color = WornColors.TextSecondary, fontSize = 15.sp, lineHeight = 22.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = WornColors.DeleteRed),
                shape = RoundedCornerShape(24.dp),
            ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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
            color = WornColors.BgCard,
            border = BorderStroke(1.dp, WornColors.BorderSubtle),
            shadowElevation = 8.dp,
            modifier = Modifier.size(size),
        ) {
            if (item.photoPath.isNotEmpty() && File(item.photoPath).exists()) {
                AsyncImage(
                    model = File(item.photoPath),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(cornerRadius)),
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.Checkroom,
                        contentDescription = null,
                        tint = WornColors.IconMuted,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
        Text(
            text = item.name,
            color = WornColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun OutfitPropertyRow(label: String, value: String, fontSize: TextUnit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = WornColors.TextSecondary,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            color = WornColors.TextPrimary,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun deriveSeasonText(items: List<ClothingItem>): String {
    val allSeasons = items.flatMap { it.seasons }.toSet()
    return when {
        allSeasons.isEmpty() -> "Not specified"
        allSeasons.size == Season.entries.size -> "All seasons"
        else -> allSeasons.joinToString("/") { it.displayName() }
    }
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

@Preview(showSystemUi = true, device = "id:pixel_8")
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

@Preview(showSystemUi = true, device = "id:pixel_tablet")
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
