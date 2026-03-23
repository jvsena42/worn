package com.github.worn.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.ui.components.CategoryFilterChips
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.WornColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOutfitSheet(
    clothingItems: List<ClothingItem>,
    selectedItemIds: Set<String>,
    activeCategory: Category?,
    isSaving: Boolean,
    onCategorySelected: (Category?) -> Unit,
    onToggleItem: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WornColors.BgElevated,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { SheetHandle() },
    ) {
        CreateOutfitForm(
            clothingItems = clothingItems,
            selectedItemIds = selectedItemIds,
            activeCategory = activeCategory,
            isSaving = isSaving,
            onCategorySelected = onCategorySelected,
            onToggleItem = onToggleItem,
            onSave = onSave,
        )
    }
}

@Composable
private fun SheetHandle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(WornColors.IconMuted),
        )
    }
}

@Composable
internal fun CreateOutfitForm(
    clothingItems: List<ClothingItem> = emptyList(),
    selectedItemIds: Set<String> = emptySet(),
    activeCategory: Category? = null,
    isSaving: Boolean = false,
    onCategorySelected: (Category?) -> Unit = {},
    onToggleItem: (String) -> Unit = {},
    onSave: (String) -> Unit = {},
) {
    var name by remember { mutableStateOf("") }
    val canSave = name.isNotBlank() && selectedItemIds.isNotEmpty() && !isSaving

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Create outfit",
            color = WornColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        OutfitNameField(name = name, onNameChange = { name = it })
        SelectItemsHeader(selectedCount = selectedItemIds.size)
        CategoryFilterChips(activeCategory = activeCategory, onCategorySelected = onCategorySelected)
        ItemSelectionGrid(
            clothingItems = clothingItems,
            selectedItemIds = selectedItemIds,
            onToggleItem = onToggleItem,
        )
        SaveOutfitButton(enabled = canSave, isSaving = isSaving, onClick = { onSave(name) })
    }
}

@Composable
private fun OutfitNameField(name: String, onNameChange: (String) -> Unit) {
    TextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text("Outfit name", color = WornColors.IconMuted, fontSize = 15.sp) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = WornColors.BgCard,
            unfocusedContainerColor = WornColors.BgCard,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WornColors.BorderSubtle, RoundedCornerShape(12.dp)),
    )
}

@Composable
private fun SelectItemsHeader(selectedCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Select items",
            color = WornColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (selectedCount > 0) {
            Text(
                text = "$selectedCount selected",
                color = WornColors.AccentGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ItemSelectionGrid(
    clothingItems: List<ClothingItem>,
    selectedItemIds: Set<String>,
    onToggleItem: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.heightIn(max = 400.dp),
    ) {
        items(clothingItems, key = { it.id }) { item ->
            SelectableItemCell(
                item = item,
                isSelected = item.id in selectedItemIds,
                onClick = { onToggleItem(item.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

private val cellShape = RoundedCornerShape(16.dp)
private val checkboxShape = RoundedCornerShape(10.dp)

@Composable
private fun SelectableItemCell(
    item: ClothingItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(cellShape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) WornColors.AccentGreen else WornColors.BorderSubtle,
                shape = cellShape,
            )
            .clickable(onClick = onClick),
    ) {
        ItemThumbnail(item = item)
        Text(
            text = item.name,
            color = WornColors.TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp),
        )
        ItemCheckbox(isSelected = isSelected, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun ItemThumbnail(item: ClothingItem) {
    Surface(shape = cellShape, color = WornColors.BgCard, shadowElevation = 4.dp, modifier = Modifier.fillMaxSize()) {
        if (item.photoPath.isNotEmpty() && File(item.photoPath).exists()) {
            AsyncImage(
                model = File(item.photoPath),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(cellShape),
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Outlined.Checkroom,
                    contentDescription = null,
                    tint = WornColors.IconMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun ItemCheckbox(isSelected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = checkboxShape,
        color = if (isSelected) WornColors.AccentGreen else WornColors.BgCard,
        border = if (isSelected) null else BorderStroke(1.5.dp, WornColors.BorderSubtle),
        modifier = modifier.size(20.dp),
    ) {
        if (isSelected) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun SaveOutfitButton(enabled: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(Color(0xFF7A9468), Color(0xFF5C6E50)))
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
                text = if (isSaving) "Saving…" else "Save outfit",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private val previewItems = listOf(
    ClothingItem("1", "Black T-Shirt", Category.TOP, listOf("black"), photoPath = "", createdAt = 0),
    ClothingItem("2", "Navy Jeans", Category.BOTTOM, listOf("navy"), photoPath = "", createdAt = 0),
    ClothingItem("3", "White Sneakers", Category.SHOES, listOf("white"), photoPath = "", createdAt = 0),
    ClothingItem("4", "Grey Hoodie", Category.TOP, listOf("grey"), photoPath = "", createdAt = 0),
    ClothingItem("5", "Olive Jacket", Category.OUTERWEAR, listOf("olive"), photoPath = "", createdAt = 0),
    ClothingItem("6", "Chinos", Category.BOTTOM, listOf("khaki"), photoPath = "", createdAt = 0),
)

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun CreateOutfitFormPhonePreview() {
    SheetPreview {
        CreateOutfitForm(
            clothingItems = previewItems,
            selectedItemIds = setOf("1", "2"),
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun CreateOutfitFormTabletPreview() {
    SheetPreview {
        CreateOutfitForm(
            clothingItems = previewItems,
            selectedItemIds = setOf("1", "2"),
        )
    }
}
