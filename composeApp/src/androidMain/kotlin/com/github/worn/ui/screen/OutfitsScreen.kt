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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.domain.model.Outfit
import com.github.worn.presentation.viewmodel.OutfitEffect
import com.github.worn.presentation.viewmodel.OutfitIntent
import com.github.worn.presentation.viewmodel.OutfitState
import com.github.worn.presentation.viewmodel.OutfitViewModel
import com.github.worn.ui.components.OutfitCard
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornBottomBar
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme
import org.koin.compose.viewmodel.koinViewModel

private val DeleteRed = Color(0xFFC45B4A)

@Composable
fun OutfitsScreen(
    onTabSelected: (Tab) -> Unit = {},
    viewModel: OutfitViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OutfitEffect.OutfitsDeleted -> {}
                is OutfitEffect.OutfitCreated -> showCreateSheet = false
                is OutfitEffect.ShowError -> {}
            }
        }
    }

    LaunchedEffect(showCreateSheet) {
        if (showCreateSheet) {
            viewModel.onIntent(OutfitIntent.LoadClothingItems)
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
        onTabSelected = onTabSelected,
    )

    if (showCreateSheet) {
        CreateOutfitSheet(
            clothingItems = state.clothingItems,
            selectedItemIds = state.selectedItemIds,
            activeCategory = state.activeItemCategory,
            isSaving = state.isSaving,
            onCategorySelected = { viewModel.onIntent(OutfitIntent.FilterItemsByCategory(it)) },
            onToggleItem = { viewModel.onIntent(OutfitIntent.ToggleItemSelection(it)) },
            onSave = { name -> viewModel.onIntent(OutfitIntent.CreateOutfit(name)) },
            onDismiss = { showCreateSheet = false },
        )
    }
}

@Composable
private fun OutfitsScaffold(
    state: OutfitState,
    isCompact: Boolean,
    onCreateClick: () -> Unit = {},
    onToggleSelection: (String) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onTabSelected: (Tab) -> Unit = {},
) {
    val isSelectionMode = state.selectedIds.isNotEmpty()
    val contentPadding = if (isCompact) 24.dp else 32.dp
    val sectionGap = if (isCompact) 24.dp else 28.dp
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WornColors.BgPage,
        bottomBar = {
            WornBottomBar(activeTab = Tab.OUTFITS, onTabSelected = onTabSelected, isCompact = isCompact)
        },
    ) { paddingValues ->
        val isEmpty = !state.isLoading && state.outfits.isEmpty()

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
                OutfitsHeader(outfitCount = state.outfits.size, onCreateClick = onCreateClick)
            }
            if (isEmpty) {
                EmptyState(onCreateClick = onCreateClick)
            } else if (state.isLoading && state.outfits.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(color = WornColors.AccentGreen)
                }
            } else {
                Spacer(modifier = Modifier.height(sectionGap))
                OutfitsContent(
                    state = state,
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
private fun OutfitsHeader(outfitCount: Int, onCreateClick: () -> Unit = {}) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Your outfits",
            color = WornColors.TextPrimary,
            fontSize = if (outfitCount == 0) 22.sp else 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        if (outfitCount > 0) {
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = WornColors.AccentGreen),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Create", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
    if (outfitCount > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$outfitCount saved combination${if (outfitCount != 1) "s" else ""}",
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
private fun OutfitsContent(
    state: OutfitState,
    onToggleSelection: (String) -> Unit,
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
                    if (isSelectionMode) onToggleSelection(outfit.id)
                },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

private val AccentGreenEnd = Color(0xFF6B8A58)
private val CtaShape = RoundedCornerShape(28.dp)
private val CtaGradient = Brush.verticalGradient(listOf(WornColors.AccentGreen, AccentGreenEnd))

@Composable
private fun EmptyState(onCreateClick: () -> Unit = {}) {
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
                imageVector = Icons.Outlined.Layers,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = WornColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No outfits yet",
            color = WornColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Create your first look by combining\nitems from your wardrobe",
            color = WornColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .shadow(elevation = 10.dp, shape = CtaShape)
                .clickable(onClick = onCreateClick)
                .background(brush = CtaGradient, shape = CtaShape)
                .padding(horizontal = 36.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp), WornColors.BgPage)
            Text(
                "Create your first outfit",
                color = WornColors.TextOnColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            )
        }
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
                "Delete $count outfit${if (count != 1) "s" else ""}?",
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
        },
        text = {
            Text(
                "This action cannot be undone. The selected outfits will be permanently removed.",
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

private val previewOutfits = listOf(
    Outfit("1", "Weekend Casual", listOf("i1", "i2", "i3", "i4"), 1_710_460_800_000),
    Outfit("2", "Office Ready", listOf("i1", "i2", "i3"), 1_710_201_600_000),
    Outfit("3", "Evening Out", listOf("i1", "i2", "i3", "i4", "i5"), 1_709_856_000_000),
)

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun OutfitsPhonePreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(outfits = previewOutfits),
            isCompact = true,
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun OutfitsSelectionPreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(outfits = previewOutfits, selectedIds = setOf("1", "3")),
            isCompact = true,
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun OutfitsEmptyPhonePreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(),
            isCompact = true,
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun OutfitsTabletPreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(outfits = previewOutfits),
            isCompact = false,
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun OutfitsEmptyTabletPreview() {
    WornTheme {
        OutfitsScaffold(
            state = OutfitState(),
            isCompact = false,
        )
    }
}
