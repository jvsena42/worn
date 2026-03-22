package com.github.worn.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

private val COMPACT_BREAKPOINT = 600.dp
private val GRID_MIN_CELL_WIDTH = 160.dp
private val GRID_GAP_COMPACT = 12.dp
private val GRID_GAP_EXPANDED = 16.dp

@Composable
fun WardrobeScreen(
    viewModel: WardrobeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WardrobeEffect.ItemAdded -> showAddSheet = false
                is WardrobeEffect.ShowError -> { /* handled elsewhere */ }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < COMPACT_BREAKPOINT
        WardrobeScaffold(
            state = state,
            isCompact = isCompact,
            onCategorySelected = { viewModel.onIntent(WardrobeIntent.FilterByCategory(it)) },
            onAddItemClick = { showAddSheet = true },
        )
    }

    if (showAddSheet) {
        AddItemSheet(
            isSaving = state.isSaving,
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
) {
    val contentPadding = if (isCompact) 24.dp else 32.dp
    val sectionGap = if (isCompact) 24.dp else 28.dp

    Scaffold(
        containerColor = WornColors.BgPage,
        floatingActionButton = { AddItemFab(onClick = onAddItemClick) },
        bottomBar = {
            WornBottomBar(
                activeTab = Tab.WARDROBE,
                onTabSelected = { },
                isCompact = isCompact,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = contentPadding),
        ) {
            WardrobeHeader(itemCount = state.items.size)
            Spacer(modifier = Modifier.height(sectionGap))
            CategoryFilterChips(
                activeCategory = state.activeCategory,
                onCategorySelected = onCategorySelected,
            )
            Spacer(modifier = Modifier.height(sectionGap))
            WardrobeContent(state = state, isCompact = isCompact)
        }
    }
}

@Composable
private fun WardrobeHeader(itemCount: Int) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Worn",
        color = WornColors.TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.8).sp,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Your capsule wardrobe \u00B7 $itemCount items",
        color = WornColors.TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun WardrobeContent(state: WardrobeState, isCompact: Boolean) {
    val gridGap = if (isCompact) GRID_GAP_COMPACT else GRID_GAP_EXPANDED
    val photoHeight: Dp = if (isCompact) 171.dp else 200.dp

    if (state.isLoading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
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
                ClothingCard(item = item, photoHeight = photoHeight)
            }
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
        Text(
            text = "Add item",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
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
