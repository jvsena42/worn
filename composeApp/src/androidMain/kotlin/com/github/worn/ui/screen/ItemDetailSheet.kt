@file:Suppress("TooManyFunctions")

package com.github.worn.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.ui.components.addItemColorPalette
import com.github.worn.ui.components.displayLabel
import com.github.worn.ui.components.displayName
import com.github.worn.ui.components.dotColor
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.WornColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    item: ClothingItem,
    isCompact: Boolean,
    onEdit: (ClothingItem) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WornColors.BgElevated,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { DetailSheetDragHandle() },
    ) {
        ItemDetailContent(item = item, isCompact = isCompact, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
private fun DetailSheetDragHandle() {
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
internal fun ItemDetailContent(
    item: ClothingItem,
    isCompact: Boolean,
    onEdit: (ClothingItem) -> Unit = {},
    onDelete: (String) -> Unit = {},
) {
    val dims = ItemDetailDimens(isCompact)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.contentPadding)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(dims.sectionGap),
    ) {
        ItemPhoto(item = item, dims = dims)
        ItemNameGroup(item = item, nameSize = dims.nameSize)
        HorizontalDivider()
        ItemProperties(item = item, fontSize = dims.propFontSize, gap = dims.propGap)
        DetailActionButtons(
            editLabel = "Edit Item",
            deleteLabel = "Delete Item",
            buttonHeight = dims.buttonHeight,
            buttonFontSize = dims.buttonFontSize,
            onEdit = { onEdit(item) },
            onDelete = { showDeleteDialog = true },
        )
    }

    if (showDeleteDialog) {
        DeleteItemDialog(
            itemName = item.name,
            onConfirm = { showDeleteDialog = false; onDelete(item.id) },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

private data class ItemDetailDimens(val isCompact: Boolean) {
    val contentPadding: Dp = if (isCompact) 24.dp else 32.dp
    val sectionGap: Dp = if (isCompact) 20.dp else 24.dp
    val photoHeight: Dp = if (isCompact) 280.dp else 360.dp
    val photoRadius: Dp = if (isCompact) 20.dp else 24.dp
    val nameSize: TextUnit = if (isCompact) 22.sp else 26.sp
    val propFontSize: TextUnit = if (isCompact) 14.sp else 15.sp
    val propGap: Dp = if (isCompact) 14.dp else 16.dp
    val buttonHeight: Dp = if (isCompact) 48.dp else 52.dp
    val buttonFontSize: TextUnit = if (isCompact) 15.sp else 16.sp
    val placeholderIconSize: Dp = if (isCompact) 64.dp else 80.dp
}

@Composable
private fun ItemPhoto(item: ClothingItem, dims: ItemDetailDimens) {
    Surface(
        shape = RoundedCornerShape(dims.photoRadius),
        color = WornColors.BgCard,
        border = BorderStroke(1.dp, WornColors.BorderSubtle),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(dims.photoHeight),
    ) {
        if (item.photoPath.isNotEmpty() && File(item.photoPath).exists()) {
            AsyncImage(
                model = File(item.photoPath),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(dims.photoRadius)),
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = null,
                    tint = WornColors.IconMuted,
                    modifier = Modifier.size(dims.placeholderIconSize),
                )
            }
        }
    }
}

@Composable
private fun ItemNameGroup(item: ClothingItem, nameSize: TextUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = item.name,
            color = WornColors.TextPrimary,
            fontSize = nameSize,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(item.category.dotColor()),
            )
            Text(
                text = item.category.displayLabel(),
                color = WornColors.TextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WornColors.BorderSubtle),
    )
}

@Composable
private fun ItemProperties(item: ClothingItem, fontSize: TextUnit, gap: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        if (item.colors.isNotEmpty()) {
            ColorPropertyRow(item = item, fontSize = fontSize)
        }
        if (item.seasons.isNotEmpty()) {
            val seasonText = if (item.seasons.size == Season.entries.size) {
                "All seasons"
            } else {
                item.seasons.joinToString(", ") { it.displayName() }
            }
            PropertyRow(label = "Season", value = seasonText, fontSize = fontSize)
        }
        item.fit?.let { PropertyRow(label = "Fit", value = it.displayName(), fontSize = fontSize) }
        item.subcategory?.let { PropertyRow(label = "Subcategory", value = it.displayName(), fontSize = fontSize) }
        item.material?.let { PropertyRow(label = "Material", value = it.displayName(), fontSize = fontSize) }
    }
}

@Composable
private fun ColorPropertyRow(item: ClothingItem, fontSize: TextUnit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Color", color = WornColors.TextSecondary, fontSize = fontSize, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = CircleShape,
                color = colorForName(item.colors.first()),
                border = BorderStroke(1.dp, WornColors.BorderSubtle),
                modifier = Modifier.size(14.dp),
            ) {}
            Text(
                text = item.colors.joinToString(", ") { it.replaceFirstChar(Char::uppercase) },
                color = WornColors.TextPrimary,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String, fontSize: TextUnit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = WornColors.TextSecondary, fontSize = fontSize, fontWeight = FontWeight.Medium)
        Text(value, color = WornColors.TextPrimary, fontSize = fontSize, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun DetailActionButtons(
    editLabel: String,
    deleteLabel: String,
    buttonHeight: Dp,
    buttonFontSize: TextUnit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = onEdit,
            shape = RoundedCornerShape(24.dp),
            color = WornColors.BgCard,
            border = BorderStroke(1.dp, WornColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth().height(buttonHeight),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    editLabel, color = WornColors.TextPrimary,
                    fontSize = buttonFontSize, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Surface(
            onClick = onDelete,
            shape = RoundedCornerShape(24.dp),
            color = WornColors.DeleteRed,
            modifier = Modifier.fillMaxWidth().height(buttonHeight),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(deleteLabel, color = Color.White, fontSize = buttonFontSize, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DeleteItemDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete item?", fontWeight = FontWeight.SemiBold, fontSize = 22.sp) },
        text = {
            Text(
                "This action cannot be undone. \"$itemName\" will be permanently removed from your wardrobe.",
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

private fun colorForName(name: String): Color {
    return addItemColorPalette.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second
        ?: Color(0xFF444444)
}

private val previewItem = ClothingItem(
    id = "1", name = "Black T-Shirt", category = Category.TOP, colors = listOf("Black"),
    seasons = listOf(Season.SPRING, Season.SUMMER, Season.FALL, Season.WINTER),
    subcategory = Subcategory.T_SHIRT, fit = Fit.REGULAR, material = Material.COTTON,
    photoPath = "", createdAt = 0,
)

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun ItemDetailSheetPhonePreview() {
    SheetPreview { ItemDetailContent(item = previewItem, isCompact = true) }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun ItemDetailSheetTabletPreview() {
    SheetPreview { ItemDetailContent(item = previewItem, isCompact = false) }
}
