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
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.ui.components.ClothingPhoto
import com.github.worn.ui.components.PropertyRow
import com.github.worn.ui.components.SheetDragHandle
import com.github.worn.ui.components.addItemColorPalette
import com.github.worn.ui.components.displayLabel
import com.github.worn.ui.components.displayName
import com.github.worn.ui.components.dotColor
import com.github.worn.ui.exposeTestTagsAsResourceId
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.TabletPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    item: ClothingItem,
    isCompact: Boolean,
    onEdit: (ClothingItem) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onDismiss: () -> Unit,
    showActions: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { SheetDragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
        ItemDetailContent(
            item = item,
            isCompact = isCompact,
            onEdit = onEdit,
            onDelete = onDelete,
            showActions = showActions,
        )
    }
}

@Composable
internal fun ItemDetailContent(
    item: ClothingItem,
    isCompact: Boolean,
    onEdit: (ClothingItem) -> Unit = {},
    onDelete: (String) -> Unit = {},
    showActions: Boolean = true,
) {
    val dims = itemDetailDimens(isCompact)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .exposeTestTagsAsResourceId()
            .testTag("item_detail_sheet")
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.contentPadding)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(dims.sectionGap),
    ) {
        ItemPhoto(item = item, dims = dims)
        ItemNameGroup(item = item, nameStyle = dims.nameStyle)
        HorizontalDivider()
        ItemProperties(item = item, textStyle = dims.propStyle, gap = dims.propGap)
        if (showActions) {
            DetailActionButtons(
                editLabel = stringResource(R.string.item_detail_edit),
                deleteLabel = stringResource(R.string.item_detail_delete),
                buttonHeight = dims.buttonHeight,
                buttonStyle = dims.buttonStyle,
                onEdit = { onEdit(item) },
                onDelete = { showDeleteDialog = true },
                editTestTag = "item_detail_edit",
                deleteTestTag = "item_detail_delete",
            )
        }
    }

    if (showDeleteDialog) {
        DeleteItemDialog(
            itemName = item.name,
            onConfirm = { showDeleteDialog = false; onDelete(item.id) },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

private data class ItemDetailDimens(
    val contentPadding: Dp,
    val sectionGap: Dp,
    val photoHeight: Dp,
    val photoRadius: Dp,
    val nameStyle: TextStyle,
    val propStyle: TextStyle,
    val propGap: Dp,
    val buttonHeight: Dp,
    val buttonStyle: TextStyle,
    val placeholderIconSize: Dp,
)

/**
 * Composable rather than a plain constructor so the text styles come from the shared type scale
 * instead of loose `sp` literals; only the spacing still varies by raw dimension.
 */
@Composable
private fun itemDetailDimens(isCompact: Boolean): ItemDetailDimens = ItemDetailDimens(
    contentPadding = if (isCompact) 24.dp else 32.dp,
    sectionGap = if (isCompact) 20.dp else 24.dp,
    photoHeight = if (isCompact) 280.dp else 360.dp,
    photoRadius = if (isCompact) 20.dp else 24.dp,
    nameStyle = if (isCompact) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.headlineSmall
    },
    propStyle = if (isCompact) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyMedium
    },
    propGap = if (isCompact) 14.dp else 16.dp,
    buttonHeight = if (isCompact) 48.dp else 52.dp,
    buttonStyle = if (isCompact) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.titleSmall
    },
    placeholderIconSize = if (isCompact) 64.dp else 80.dp,
)

@Composable
private fun ItemPhoto(item: ClothingItem, dims: ItemDetailDimens) {
    Surface(
        shape = RoundedCornerShape(dims.photoRadius),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(dims.photoHeight),
    ) {
        ClothingPhoto(
            photoPath = item.photoPath,
            contentDescription = item.name,
            shape = RoundedCornerShape(dims.photoRadius),
            placeholderIconSize = dims.placeholderIconSize,
        )
    }
}

@Composable
private fun ItemNameGroup(item: ClothingItem, nameStyle: TextStyle) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = item.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = nameStyle,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
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
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun ItemProperties(item: ClothingItem, textStyle: TextStyle, gap: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        if (item.colors.isNotEmpty()) {
            ColorPropertyRow(item = item, textStyle = textStyle)
        }
        if (item.seasons.isNotEmpty()) {
            val seasonText = if (item.seasons.size == Season.entries.size) {
                stringResource(R.string.common_all_seasons)
            } else {
                item.seasons.map { it.displayName() }.joinToString(", ")
            }
            PropertyRow(label = stringResource(R.string.label_season), value = seasonText, textStyle = textStyle)
        }
        item.fit?.let {
            PropertyRow(label = stringResource(R.string.label_fit), value = it.displayName(), textStyle = textStyle)
        }
        item.subcategory?.let {
            PropertyRow(
                label = stringResource(R.string.label_subcategory),
                value = it.displayName(),
                textStyle = textStyle,
            )
        }
        item.material?.let {
            PropertyRow(
                label = stringResource(R.string.label_material),
                value = it.displayName(),
                textStyle = textStyle,
            )
        }
    }
}

@Composable
private fun ColorPropertyRow(item: ClothingItem, textStyle: TextStyle) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.label_color),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = textStyle,
            fontWeight = FontWeight.Medium,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = CircleShape,
                color = colorForName(item.colors.first()),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(14.dp),
            ) {}
            Text(
                text = item.colors.joinToString(", ") { it.replaceFirstChar(Char::uppercase) },
                color = MaterialTheme.colorScheme.onSurface,
                style = textStyle,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}


@Composable
internal fun DetailActionButtons(
    editLabel: String,
    deleteLabel: String,
    buttonHeight: Dp,
    buttonStyle: TextStyle,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    editTestTag: String,
    deleteTestTag: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = onEdit,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().height(buttonHeight).testTag(editTestTag),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    editLabel, color = MaterialTheme.colorScheme.onSurface,
                    style = buttonStyle, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Surface(
            onClick = onDelete,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth().height(buttonHeight).testTag(deleteTestTag),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(deleteLabel, color = Color.White, style = buttonStyle, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DeleteItemDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.item_detail_delete_dialog_title),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                stringResource(R.string.item_detail_delete_dialog_message, itemName),
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

@PhonePreview
@Composable
private fun ItemDetailSheetPhonePreview() {
    SheetPreview { ItemDetailContent(item = previewItem, isCompact = true) }
}

@TabletPreview
@Composable
private fun ItemDetailSheetTabletPreview() {
    SheetPreview { ItemDetailContent(item = previewItem, isCompact = false) }
}

