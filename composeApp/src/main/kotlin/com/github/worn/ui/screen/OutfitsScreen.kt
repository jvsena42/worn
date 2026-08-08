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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.R
import com.github.worn.domain.model.Outfit
import com.github.worn.presentation.viewmodel.OutfitEffect
import com.github.worn.presentation.viewmodel.OutfitIntent
import com.github.worn.presentation.viewmodel.OutfitState
import com.github.worn.presentation.viewmodel.OutfitViewModel
import com.github.worn.ui.components.DeleteConfirmationDialog
import com.github.worn.ui.components.EmptyStateView
import com.github.worn.ui.components.OutfitCard
import com.github.worn.ui.components.SelectionHeader
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornGradientButton
import com.github.worn.ui.components.WornGradients
import com.github.worn.ui.components.WornTopAppBar
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornTheme
import org.koin.compose.viewmodel.koinViewModel

@Suppress("UnusedParameter")
@Composable
fun OutfitsScreen(
    onTabSelected: (Tab) -> Unit = {},
    viewModel: OutfitViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateSheet by remember { mutableStateOf(false) }
    var detailOutfit by remember { mutableStateOf<Outfit?>(null) }
    var editOutfit by remember { mutableStateOf<Outfit?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OutfitEffect.OutfitCreated, is OutfitEffect.OutfitUpdated -> {
                    showCreateSheet = false; editOutfit = null
                }
                is OutfitEffect.OutfitDeleted -> detailOutfit = null
                is OutfitEffect.OutfitsDeleted, is OutfitEffect.ShowError -> {}
            }
        }
    }

    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    OutfitsScaffold(
        state = state,
        isCompact = isCompact,
        onCreateClick = { showCreateSheet = true },
        onToggleSelection = { viewModel.onIntent(OutfitIntent.ToggleSelection(it)) },
        onClearSelection = { viewModel.onIntent(OutfitIntent.ClearSelection) },
        onDeleteSelected = { viewModel.onIntent(OutfitIntent.DeleteSelected) },
        onOutfitClick = { detailOutfit = it },
    )

    if (showCreateSheet) {
        OutfitCreateSheet(
            state = state,
            editOutfit = editOutfit,
            onIntent = viewModel::onIntent,
            onDismiss = { showCreateSheet = false; editOutfit = null },
        )
    }

    detailOutfit?.let { outfit ->
        OutfitDetailSheet(
            outfit = outfit,
            clothingItems = state.allClothingItems,
            isCompact = isCompact,
            onEdit = { editingOutfit ->
                detailOutfit = null
                editOutfit = editingOutfit
                editingOutfit.itemIds.forEach { itemId ->
                    if (itemId !in state.selectedItemIds) viewModel.onIntent(OutfitIntent.ToggleItemSelection(itemId))
                }
                showCreateSheet = true
            },
            onDelete = { viewModel.onIntent(OutfitIntent.DeleteOutfit(it)) },
            onDismiss = { detailOutfit = null },
        )
    }
}

@Composable
private fun OutfitCreateSheet(
    state: OutfitState,
    editOutfit: Outfit?,
    onIntent: (OutfitIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    CreateOutfitSheet(
        clothingItems = state.clothingItems,
        selectedItemIds = state.selectedItemIds,
        activeCategory = state.activeItemCategory,
        isSaving = state.isSaving,
        existingOutfit = editOutfit,
        onCategorySelected = { onIntent(OutfitIntent.FilterItemsByCategory(it)) },
        onToggleItem = { onIntent(OutfitIntent.ToggleItemSelection(it)) },
        onSave = { name ->
            if (editOutfit != null) {
                val updated = editOutfit.copy(name = name, itemIds = state.selectedItemIds.toList())
                onIntent(OutfitIntent.UpdateOutfit(updated))
            } else {
                onIntent(OutfitIntent.CreateOutfit(name))
            }
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun OutfitsScaffold(
    state: OutfitState,
    isCompact: Boolean,
    onCreateClick: () -> Unit = {},
    onToggleSelection: (String) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onOutfitClick: (Outfit) -> Unit = {},
) {
    val isSelectionMode = state.selectedIds.isNotEmpty()
    val contentPadding = if (isCompact) 24.dp else 32.dp
    val sectionGap = if (isCompact) 24.dp else 28.dp
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .testTag("outfits_screen")
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            if (!isSelectionMode && state.outfits.isNotEmpty()) {
                CreateOutfitFab(onCreateClick, Modifier.testTag("outfits_create_button"))
            }
        },
        topBar = {
            if (isSelectionMode) {
                SelectionHeader(
                    count = state.selectedIds.size,
                    onCancel = onClearSelection,
                    onDelete = { showDeleteDialog = true },
                    modifier = Modifier.padding(horizontal = contentPadding),
                )
            } else {
                OutfitsTopBar(outfitCount = state.outfits.size, scrollBehavior = scrollBehavior)
            }
        },
    ) { paddingValues ->
        val isEmpty = !state.isLoading && state.outfits.isEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = contentPadding),
        ) {
            if (isEmpty) {
                EmptyState(onCreateClick = onCreateClick)
            } else if (state.isLoading && state.outfits.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    LoadingIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Spacer(modifier = Modifier.height(sectionGap))
                OutfitsContent(
                    state = state,
                    onToggleSelection = onToggleSelection,
                    onOutfitClick = onOutfitClick,
                )
            }
        }
    }

    if (showDeleteDialog) DeleteConfirmationDialog(
        title = pluralStringResource(R.plurals.delete_outfits_title, state.selectedIds.size, state.selectedIds.size),
        message = stringResource(R.string.outfits_delete_dialog_message),
        isDeleting = state.isDeleting,
        onConfirm = { onDeleteSelected(); showDeleteDialog = false },
        onDismiss = { showDeleteDialog = false },
    )
}

@Composable
private fun OutfitsTopBar(outfitCount: Int, scrollBehavior: TopAppBarScrollBehavior) {
    // Title unchanged: journeys/create-first-outfit.xml asserts on it.
    WornTopAppBar(
        title = stringResource(R.string.outfits_title),
        subtitle = if (outfitCount > 0) {
            pluralStringResource(R.plurals.saved_combinations, outfitCount, outfitCount)
        } else {
            null
        },
        scrollBehavior = scrollBehavior,
    )
}

/**
 * Create as a FAB rather than a top-bar action.
 *
 * As an action it sat alone in the app bar's otherwise empty leading row, reading as a button
 * floating above the title. A FAB also matches Wardrobe's "Add item", so the two list screens
 * now offer their primary action in the same place.
 */
@Composable
private fun CreateOutfitFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            text = stringResource(R.string.outfits_button_create),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}


@Composable
private fun OutfitsContent(
    state: OutfitState,
    onToggleSelection: (String) -> Unit,
    onOutfitClick: (Outfit) -> Unit = {},
) {
    val isSelectionMode = state.selectedIds.isNotEmpty()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.outfits, key = { it.id }) { outfit ->
            OutfitCard(
                outfit = outfit,
                itemCategories = state.itemCategories,
                isSelected = outfit.id in state.selectedIds,
                isSelectionMode = isSelectionMode,
                onLongPress = { onToggleSelection(outfit.id) },
                onClick = {
                    if (isSelectionMode) onToggleSelection(outfit.id) else onOutfitClick(outfit)
                },
                modifier = Modifier
                    .testTag("outfit_card")
                    .animateItem(),
            )
        }
    }
}

private val CtaShape: Shape
    @Composable @ReadOnlyComposable get() = MaterialTheme.shapes.extraLargeIncreased

@Composable
private fun EmptyState(onCreateClick: () -> Unit = {}) {
    EmptyStateView(
        icon = {
            Icon(
                imageVector = Icons.Outlined.Layers,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        title = stringResource(R.string.outfits_empty_title),
        description = stringResource(R.string.outfits_empty_description),
        action = {
            WornGradientButton(
                text = stringResource(R.string.outfits_empty_cta),
                onClick = onCreateClick,
                modifier = Modifier.testTag("outfits_empty_cta"),
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


private val previewOutfits = listOf(
    Outfit("1", "Weekend Casual", listOf("i1", "i2", "i3", "i4"), 1_710_460_800_000),
    Outfit("2", "Office Ready", listOf("i1", "i2", "i3"), 1_710_201_600_000),
    Outfit("3", "Evening Out", listOf("i1", "i2", "i3", "i4", "i5"), 1_709_856_000_000),
)

@PhonePreview
@Composable
private fun OutfitsPhonePreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(outfits = previewOutfits),
            isCompact = true,
        )
    }
}

@PhonePreview
@Composable
private fun OutfitsSelectionPreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(outfits = previewOutfits, selectedIds = setOf("1", "3")),
            isCompact = true,
        )
    }
}

@PhonePreview
@Composable
private fun OutfitsEmptyPhonePreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(),
            isCompact = true,
        )
    }
}

@TabletPreview
@Composable
private fun OutfitsTabletPreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(outfits = previewOutfits),
            isCompact = false,
        )
    }
}

@TabletPreview
@Composable
private fun OutfitsEmptyTabletPreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(),
            isCompact = false,
        )
    }
}





