@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.github.worn.ui.screen

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.presentation.viewmodel.WardrobeEffect
import com.github.worn.presentation.viewmodel.WardrobeIntent
import com.github.worn.presentation.viewmodel.WardrobeState
import com.github.worn.presentation.viewmodel.WardrobeViewModel
import com.github.worn.ui.components.CategoryFilterChips
import com.github.worn.ui.components.ClothingCard
import com.github.worn.ui.components.DeleteConfirmationDialog
import com.github.worn.ui.components.EmptyStateView
import com.github.worn.ui.components.SelectionHeader
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornGradientButton
import com.github.worn.ui.components.WornGradients
import com.github.worn.ui.components.WornTopAppBarTitlePadding
import com.github.worn.ui.components.wornTopAppBarColors
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornTheme
import com.github.worn.ui.util.ShortcutCommand
import org.koin.compose.viewmodel.koinViewModel

private val GRID_MIN_CELL_WIDTH = 160.dp
private val GRID_GAP_COMPACT = 12.dp
private val GRID_GAP_EXPANDED = 16.dp
@Composable
fun WardrobeScreen(
    onTabSelected: (Tab) -> Unit = {},
    openAddSheet: ShortcutCommand? = null,
    onAddSheetOpened: () -> Unit = {},
    viewModel: WardrobeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<ClothingItem?>(null) }
    var editItem by remember { mutableStateOf<ClothingItem?>(null) }

    /**
     * [onAddSheetOpened] clears the shortcut so it is handled exactly once: the pager disposes this
     * screen when it is more than a page away, and without this the sheet would reopen on every
     * return to the tab.
     */
    LaunchedEffect(openAddSheet) {
        if (openAddSheet == null) return@LaunchedEffect
        editItem = null
        showAddSheet = true
        onAddSheetOpened()
    }

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
        isAiAvailable = state.isAiAvailable,
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

    // exitUntilCollapsed: the title shrinks to a compact bar as the grid scrolls up and only
    // returns once the user scrolls back to the top, which is the standard large-app-bar feel.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .testTag("wardrobe_screen")
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            if (isSelectionMode) {
                SelectionHeader(
                    count = state.selectedIds.size,
                    onCancel = onClearSelection,
                    onDelete = { showDeleteDialog = true },
                    modifier = Modifier.padding(horizontal = contentPadding),
                )
            } else {
                WardrobeTopBar(itemCount = state.totalItemCount, scrollBehavior = scrollBehavior)
            }
        },
        floatingActionButton = {
            val isWardrobeEmpty = !state.isLoading && state.totalItemCount == 0
            if (!isSelectionMode && !isWardrobeEmpty) {
                AddItemFab(
                    onAddItemClick,
                    Modifier
                        .testTag("wardrobe_add_fab")
                )
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
        title = pluralStringResource(R.plurals.delete_items_title, state.selectedIds.size, state.selectedIds.size),
        message = stringResource(R.string.wardrobe_delete_dialog_message),
        isDeleting = state.isDeleting,
        onConfirm = { onDeleteSelected(); showDeleteDialog = false },
        onDismiss = { showDeleteDialog = false },
    )
}

@Composable
private fun WardrobeTopBar(itemCount: Int, scrollBehavior: TopAppBarScrollBehavior) {
    // Title strings are unchanged: journeys/bottom-navigation.xml and add-first-item.xml assert
    // on the visible heading text.
    LargeFlexibleTopAppBar(
        title = {
            Text(
                modifier = WornTopAppBarTitlePadding,
                text = if (itemCount == 0) {
                    stringResource(R.string.wardrobe_title_empty)
                } else {
                    stringResource(R.string.wardrobe_title)
                },
            )
        },
        subtitle = if (itemCount > 0) {
            { Text(stringResource(R.string.wardrobe_subtitle, itemCount), modifier = WornTopAppBarTitlePadding) }
        } else {
            null
        },
        colors = wornTopAppBarColors(),
        scrollBehavior = scrollBehavior,
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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                        if (isSelectionMode) onToggleSelection(item.id) else onItemClick(item)
                    },
                    modifier = Modifier
                        .testTag("clothing_card")
                        .animateItem(),
                )
            }
        }
    }
}

private val CtaShape: Shape
    @Composable @ReadOnlyComposable get() = MaterialTheme.shapes.extraLargeIncreased

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
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.wardrobe_category_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        title = stringResource(R.string.wardrobe_empty_title),
        description = stringResource(R.string.wardrobe_empty_description),
        action = {
            WornGradientButton(
                text = stringResource(R.string.wardrobe_empty_cta),
                onClick = onAddItemClick,
                modifier = Modifier.testTag("wardrobe_empty_add_cta"),
                gradientColors = WornGradients.GreenCta,
                shape = CtaShape,
                elevation = 10.dp,
                fillMaxWidth = false,
                fixedHeight = null,
                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 16.dp),
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        Modifier.size(18.dp),
                        MaterialTheme.colorScheme.surface,
                    )
                },
            )
        },
    )
}

@Composable
private fun AddItemFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.wardrobe_fab_add),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}


private val previewItems = listOf(
    ClothingItem("1", "Black T-Shirt", Category.TOP, listOf("black"), photoPath = "", createdAt = 0),
    ClothingItem("2", "Navy Jeans", Category.BOTTOM, listOf("navy"), photoPath = "", createdAt = 0),
    ClothingItem("3", "White Sneakers", Category.SHOES, listOf("white"), photoPath = "", createdAt = 0),
    ClothingItem("4", "Olive Jacket", Category.OUTERWEAR, listOf("olive"), photoPath = "", createdAt = 0),
    ClothingItem("5", "Grey Hoodie", Category.TOP, listOf("grey"), photoPath = "", createdAt = 0),
    ClothingItem("6", "Chinos", Category.BOTTOM, listOf("khaki"), photoPath = "", createdAt = 0),
)

@PhonePreview
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

@PhonePreview
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

@PhonePreview
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

@TabletPreview
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

@TabletPreview
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

@PhonePreview
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

@TabletPreview
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



