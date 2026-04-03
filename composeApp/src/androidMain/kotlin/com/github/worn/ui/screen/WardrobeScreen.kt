package com.github.worn.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.github.worn.ui.components.EmptyStateView
import com.github.worn.ui.components.WornGradientButton
import com.github.worn.ui.components.WornGradients
import com.github.worn.ui.components.ClothingCard
import com.github.worn.ui.components.Tab
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornDimens
import com.github.worn.ui.theme.WornTheme
import org.koin.compose.viewmodel.koinViewModel

private val GRID_MIN_CELL_WIDTH = 160.dp
private val GRID_GAP_COMPACT = 12.dp
private val GRID_GAP_EXPANDED = 16.dp
@Composable
fun WardrobeScreen(
    onTabSelected: (Tab) -> Unit = {},
    viewModel: WardrobeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<ClothingItem?>(null) }
    var editItem by remember { mutableStateOf<ClothingItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WardrobeEffect.ItemAdded -> {
                    showAddSheet = false
                    editItem = null
                }
                is WardrobeEffect.ItemUpdated -> {
                    showAddSheet = false
                    editItem = null
                }
                is WardrobeEffect.ItemsDeleted -> {}
                is WardrobeEffect.ItemDeleted -> detailItem = null
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
        onItemClick = { detailItem = it },
    )

    if (showAddSheet) {
        WardrobeAddItemSheet(
            state = state,
            editItem = editItem,
            onIntent = viewModel::onIntent,
            onDismiss = { showAddSheet = false; editItem = null },
        )
    }

    detailItem?.let { item ->
        ItemDetailSheet(
            item = item,
            isCompact = isCompact,
            onEdit = { detailItem = null; editItem = it; showAddSheet = true },
            onDelete = { viewModel.onIntent(WardrobeIntent.DeleteItem(it)) },
            onDismiss = { detailItem = null },
        )
    }
}

@Composable
private fun WardrobeAddItemSheet(
    state: WardrobeState,
    editItem: ClothingItem?,
    onIntent: (WardrobeIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    AddItemSheet(
        isSaving = state.isSaving,
        hasApiKey = state.hasApiKey,
        existingItem = editItem,
        onSave = { imageBytes, name, category, colors, seasons, subcategory, fit, material ->
            if (editItem != null) {
                onIntent(
                    WardrobeIntent.UpdateItem(
                        editItem.copy(
                            name = name, category = category, colors = colors,
                            seasons = seasons, subcategory = subcategory,
                            fit = fit, material = material,
                        ),
                    ),
                )
            } else {
                onIntent(
                    WardrobeIntent.AddItem(imageBytes, name, category, colors, seasons, subcategory, fit, material),
                )
            }
        },
        onDismiss = onDismiss,
    )
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
    onItemClick: (ClothingItem) -> Unit = {},
) {
    val isSelectionMode = state.selectedIds.isNotEmpty()
    val contentPadding = if (isCompact) 24.dp else 32.dp
    val sectionGap = if (isCompact) 24.dp else 28.dp
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WornColors.BgPage,
        floatingActionButton = {
            val isWardrobeEmpty = !state.isLoading && state.totalItemCount == 0
            if (!isSelectionMode && !isWardrobeEmpty) {
                AddItemFab(onAddItemClick, Modifier.padding(bottom = WornDimens.BottomBarClearance))
            }
        },
    ) { paddingValues ->
        val isWardrobeEmpty = !state.isLoading && state.totalItemCount == 0
        val isCategoryEmpty = !state.isLoading && state.items.isEmpty() && state.totalItemCount > 0

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
                WardrobeHeader(itemCount = state.totalItemCount)
            }
            if (isWardrobeEmpty) {
                EmptyState(onAddItemClick = onAddItemClick)
            } else {
                Spacer(modifier = Modifier.height(sectionGap))
                CategoryFilterChips(
                    activeCategory = state.activeCategory,
                    onCategorySelected = onCategorySelected,
                )
                Spacer(modifier = Modifier.height(sectionGap))
                if (isCategoryEmpty) {
                    CategoryEmptyState()
                } else {
                    WardrobeContent(
                        state = state,
                        isCompact = isCompact,
                        onToggleSelection = onToggleSelection,
                        onItemClick = onItemClick,
                    )
                }
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
        text = if (itemCount == 0) {
            stringResource(R.string.wardrobe_title_empty)
        } else {
            stringResource(R.string.wardrobe_title)
        },
        color = WornColors.TextPrimary,
        fontSize = if (itemCount == 0) 22.sp else 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
    )
    if (itemCount > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wardrobe_subtitle, itemCount),
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
            text = pluralStringResource(R.plurals.selected_count, count, count),
            color = WornColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.8).sp,
        )
        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = WornColors.DeleteRed),
            shape = RoundedCornerShape(22.dp),
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.common_delete),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.common_cancel),
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
    onItemClick: (ClothingItem) -> Unit = {},
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
            contentPadding = PaddingValues(bottom = WornDimens.BottomBarClearance),
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
                        if (isSelectionMode) onToggleSelection(item.id) else onItemClick(item)
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

private val CtaShape = RoundedCornerShape(28.dp)
private val CtaGradient = Brush.verticalGradient(listOf(WornColors.AccentGreen, WornColors.AccentGreenEnd))

@Composable
private fun CategoryEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_shirt),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = WornColors.TextSecondary.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.wardrobe_category_empty),
            color = WornColors.TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyState(onAddItemClick: () -> Unit) {
    EmptyStateView(
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_shirt),
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = WornColors.TextSecondary,
            )
        },
        title = stringResource(R.string.wardrobe_empty_title),
        description = stringResource(R.string.wardrobe_empty_description),
        action = {
            WornGradientButton(
                text = stringResource(R.string.wardrobe_empty_cta),
                onClick = onAddItemClick,
                gradientColors = WornGradients.GreenCta,
                shape = CtaShape,
                elevation = 10.dp,
                fillMaxWidth = false,
                fixedHeight = null,
                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 16.dp),
                icon = {
                    Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp), WornColors.BgPage)
                },
            )
        },
    )
}

@Composable
private fun AddItemFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = WornColors.AccentGreen,
        contentColor = WornColors.TextOnColor,
        shape = RoundedCornerShape(30.dp),
        modifier = modifier,
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.wardrobe_fab_add), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
                pluralStringResource(R.plurals.delete_items_title, count, count),
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
        },
        text = {
            Text(
                stringResource(R.string.wardrobe_delete_dialog_message),
                color = WornColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = WornColors.DeleteRed),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    text = if (isDeleting) {
                        stringResource(R.string.common_deleting)
                    } else {
                        stringResource(R.string.common_delete)
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
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
            state = WardrobeState(items = previewItems, totalItemCount = previewItems.size),
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
            state = WardrobeState(
                items = previewItems,
                selectedIds = setOf("1", "3"),
                totalItemCount = previewItems.size,
            ),
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
            state = WardrobeState(items = previewItems, totalItemCount = previewItems.size),
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

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun WardrobeEmptyCategoryPhonePreview() {
    WornTheme {
        WardrobeScaffold(
            state = WardrobeState(
                activeCategory = Category.TOP,
                totalItemCount = previewItems.size,
            ),
            isCompact = true,
            onCategorySelected = {},
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun WardrobeEmptyCategoryTabletPreview() {
    WornTheme {
        WardrobeScaffold(
            state = WardrobeState(
                activeCategory = Category.TOP,
                totalItemCount = previewItems.size,
            ),
            isCompact = false,
            onCategorySelected = {},
        )
    }
}
