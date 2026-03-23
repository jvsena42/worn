package com.github.worn.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.presentation.viewmodel.WardrobeEffect
import com.github.worn.presentation.viewmodel.WardrobeIntent
import com.github.worn.presentation.viewmodel.WardrobeState
import com.github.worn.presentation.viewmodel.WardrobeViewModel
import com.github.worn.ui.components.CategoryFilterChips
import com.github.worn.ui.components.ClothingCard
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornBottomBar
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme
import org.koin.compose.viewmodel.koinViewModel

private val GRID_MIN_CELL_WIDTH = 160.dp
private val GRID_GAP_COMPACT = 12.dp
private val GRID_GAP_EXPANDED = 16.dp
private val DeleteRed = Color(0xFFC45B4A)

@Composable
fun WardrobeScreen(viewModel: WardrobeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WardrobeEffect.ItemAdded -> showAddSheet = false
                is WardrobeEffect.ItemsDeleted -> {}
                is WardrobeEffect.ShowError -> {}
            }
        }
    }

    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    WardrobeScaffold(
        state = state,
        isCompact = isCompact,
        onCategorySelected = { viewModel.onIntent(WardrobeIntent.FilterByCategory(it)) },
        onAddItemClick = { showAddSheet = true },
        onToggleSelection = { viewModel.onIntent(WardrobeIntent.ToggleSelection(it)) },
        onClearSelection = { viewModel.onIntent(WardrobeIntent.ClearSelection) },
        onDeleteSelected = { viewModel.onIntent(WardrobeIntent.DeleteSelected) },
    )

    if (showAddSheet) {
        AddItemSheet(
            isSaving = state.isSaving,
            hasApiKey = state.hasApiKey,
            onSave = { imageBytes, name, category, colors, seasons ->
                viewModel.onIntent(
                    WardrobeIntent.AddItem(imageBytes, name, category, colors, seasons),
                )
            },
            onDismiss = { showAddSheet = false },
        )
    }
}

@Composable
private fun WardrobeScaffold(
    state: WardrobeState,
    isCompact: Boolean,
    onCategorySelected: (Category?) -> Unit,
    onAddItemClick: () -> Unit = {},
    onToggleSelection: (String) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
) {
    val isSelectionMode = state.selectedIds.isNotEmpty()
    val contentPadding = if (isCompact) 24.dp else 32.dp
    val sectionGap = if (isCompact) 24.dp else 28.dp
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WornColors.BgPage,
        floatingActionButton = {
            if (!isSelectionMode) AddItemFab(onClick = onAddItemClick)
        },
        bottomBar = {
            WornBottomBar(activeTab = Tab.WARDROBE, onTabSelected = {}, isCompact = isCompact)
        },
    ) { paddingValues ->
        val isEmpty = !state.isLoading && state.items.isEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = contentPadding),
        ) {
            if (isSelectionMode) {
                SelectionHeader(
                    count = state.selectedIds.size,
                    onCancel = onClearSelection,
                    onDelete = { showDeleteDialog = true },
                )
            } else {
                WardrobeHeader(itemCount = state.items.size)
            }
            if (isEmpty) {
                EmptyState(onAddItemClick = onAddItemClick)
            } else {
                Spacer(modifier = Modifier.height(sectionGap))
                CategoryFilterChips(
                    activeCategory = state.activeCategory,
                    onCategorySelected = onCategorySelected,
                )
                Spacer(modifier = Modifier.height(sectionGap))
                WardrobeContent(
                    state = state,
                    isCompact = isCompact,
                    onToggleSelection = onToggleSelection,
                )
            }
        }
    }

    if (showDeleteDialog) DeleteConfirmationDialog(
        count = state.selectedIds.size,
        isDeleting = state.isDeleting,
        onConfirm = { onDeleteSelected(); showDeleteDialog = false },
        onDismiss = { showDeleteDialog = false },
    )
}

@Composable
private fun WardrobeHeader(itemCount: Int) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = if (itemCount == 0) "Your wardrobe" else "Worn",
        color = WornColors.TextPrimary,
        fontSize = if (itemCount == 0) 22.sp else 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
    )
    if (itemCount > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your capsule wardrobe \u00B7 $itemCount items",
            color = WornColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SelectionHeader(count: Int, onCancel: () -> Unit, onDelete: () -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$count selected",
            color = WornColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.8).sp,
        )
        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
            shape = RoundedCornerShape(22.dp),
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Cancel",
        color = WornColors.TextSecondary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.clickable(onClick = onCancel),
    )
}

@Composable
private fun WardrobeContent(
    state: WardrobeState,
    isCompact: Boolean,
    onToggleSelection: (String) -> Unit,
) {
    val gridGap = if (isCompact) GRID_GAP_COMPACT else GRID_GAP_EXPANDED
    val photoHeight: Dp = if (isCompact) 171.dp else 200.dp
    val isSelectionMode = state.selectedIds.isNotEmpty()

    if (state.isLoading && state.items.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(color = WornColors.AccentGreen)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = GRID_MIN_CELL_WIDTH),
            horizontalArrangement = Arrangement.spacedBy(gridGap),
            verticalArrangement = Arrangement.spacedBy(gridGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.items, key = { it.id }) { item ->
                ClothingCard(
                    item = item,
                    photoHeight = photoHeight,
                    isSelected = item.id in state.selectedIds,
                    isSelectionMode = isSelectionMode,
                    onLongPress = { onToggleSelection(item.id) },
                    onClick = {
                        if (isSelectionMode) onToggleSelection(item.id)
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

private val AccentGreenEnd = Color(0xFF6B8A58)
private val CtaShape = RoundedCornerShape(28.dp)
private val CtaGradient = Brush.verticalGradient(listOf(WornColors.AccentGreen, AccentGreenEnd))

@Composable
private fun EmptyState(onAddItemClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(130.dp)
                .shadow(15.dp, CircleShape)
                .background(WornColors.BgCard, CircleShape)
                .border(1.dp, WornColors.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shirt),
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = WornColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No items yet",
            color = WornColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Add your first piece to start\nbuilding your wardrobe",
            color = WornColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .shadow(elevation = 10.dp, shape = CtaShape)
                .clickable(onClick = onAddItemClick)
                .background(brush = CtaGradient, shape = CtaShape)
                .padding(horizontal = 36.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp), WornColors.BgPage)
            Text(
                "Add your first item",
                color = WornColors.TextOnColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AddItemFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = WornColors.AccentGreen,
        contentColor = WornColors.TextOnColor,
        shape = RoundedCornerShape(30.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text = "Add item", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun DeleteConfirmationDialog(
    count: Int,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Delete $count item${if (count != 1) "s" else ""}?",
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
        },
        text = {
            Text(
                "This action cannot be undone. The selected items will be permanently removed from your wardrobe.",
                color = WornColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(if (isDeleting) "Deleting…" else "Delete", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private val previewItems = listOf(
    ClothingItem("1", "Black T-Shirt", Category.TOP, listOf("black"), photoPath = "", createdAt = 0),
    ClothingItem("2", "Navy Jeans", Category.BOTTOM, listOf("navy"), photoPath = "", createdAt = 0),
    ClothingItem("3", "White Sneakers", Category.SHOES, listOf("white"), photoPath = "", createdAt = 0),
    ClothingItem("4", "Olive Jacket", Category.OUTERWEAR, listOf("olive"), photoPath = "", createdAt = 0),
    ClothingItem("5", "Grey Hoodie", Category.TOP, listOf("grey"), photoPath = "", createdAt = 0),
    ClothingItem("6", "Chinos", Category.BOTTOM, listOf("khaki"), photoPath = "", createdAt = 0),
)

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun WardrobeScreenPhonePreview() {
    WornTheme {
        WardrobeScaffold(
            state = WardrobeState(items = previewItems),
            isCompact = true,
            onCategorySelected = {},
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun WardrobeSelectModePreview() {
    WornTheme {
        WardrobeScaffold(
            state = WardrobeState(items = previewItems, selectedIds = setOf("1", "3")),
            isCompact = true,
            onCategorySelected = {},
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun WardrobeEmptyPhonePreview() {
    WornTheme {
        WardrobeScaffold(
            state = WardrobeState(),
            isCompact = true,
            onCategorySelected = {},
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun WardrobeScreenTabletPreview() {
    WornTheme {
        WardrobeScaffold(
            state = WardrobeState(items = previewItems),
            isCompact = false,
            onCategorySelected = {},
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun WardrobeEmptyTabletPreview() {
    WornTheme {
        WardrobeScaffold(
            state = WardrobeState(),
            isCompact = false,
            onCategorySelected = {},
        )
    }
}
