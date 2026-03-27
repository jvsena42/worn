@file:Suppress("TooManyFunctions")

package com.github.worn.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.presentation.viewmodel.GapsState
import com.github.worn.presentation.viewmodel.GapsViewModel
import com.github.worn.ui.components.AiLockedSheet
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornBottomBar
import com.github.worn.ui.components.iconRes
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GapsScreen(onTabSelected: (Tab) -> Unit) {
    val viewModel: GapsViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    var selectedGap by remember { mutableStateOf<GapRecommendation?>(null) }
    var showAiLockedSheet by remember { mutableStateOf(false) }
    var showAddItemSheet by remember { mutableStateOf(false) }
    var addItemPreFill by remember { mutableStateOf<GapRecommendation?>(null) }

    GapsScaffold(
        state = state,
        isCompact = isCompact,
        onCardClick = { selectedGap = it },
        onBannerClick = { if (!state.isAiMode) showAiLockedSheet = true },
        onTabSelected = onTabSelected,
    )

    if (selectedGap != null) {
        GapDetailSheet(
            recommendation = selectedGap!!,
            isAiMode = state.isAiMode,
            onAddToWardrobe = {
                addItemPreFill = selectedGap
                selectedGap = null
                showAddItemSheet = true
            },
            onDismiss = { selectedGap = null },
        )
    }

    if (showAiLockedSheet) {
        AiLockedSheet(
            onDismiss = { showAiLockedSheet = false },
            onGoToSettings = {
                showAiLockedSheet = false
                onTabSelected(Tab.SETTINGS)
            },
        )
    }

    if (showAddItemSheet && addItemPreFill != null) {
        val gap = addItemPreFill!!
        AddItemSheet(
            isSaving = false,
            hasApiKey = state.hasApiKey,
            existingItem = gap.toPreFilledItem(),
            onSave = { _, _, _, _, _, _, _, _ -> showAddItemSheet = false },
            onDismiss = { showAddItemSheet = false },
        )
    }
}

@Composable
private fun GapsScaffold(
    state: GapsState,
    isCompact: Boolean = true,
    onCardClick: (GapRecommendation) -> Unit = {},
    onBannerClick: () -> Unit = {},
    onTabSelected: (Tab) -> Unit = {},
) {
    val contentPadding = if (isCompact) 24.dp else 32.dp

    Scaffold(
        containerColor = WornColors.BgPage,
        bottomBar = {
            WornBottomBar(
                activeTab = Tab.GAPS,
                onTabSelected = onTabSelected,
                isCompact = isCompact,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "What's missing",
                color = WornColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                text = "Items that would expand your combinations most",
                color = WornColors.TextSecondary,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(20.dp))

            when {
                state.isLoading -> LoadingContent()
                state.recommendations.isEmpty() -> CompleteContent()
                else -> GapsContent(
                    state = state,
                    onCardClick = onCardClick,
                    onBannerClick = onBannerClick,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
    ) {
        CircularProgressIndicator(color = WornColors.AccentGreen)
    }
}

@Composable
private fun CompleteContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(WornColors.BgElevated),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = WornColors.AccentGreen,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Your wardrobe looks complete!",
            color = WornColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "We couldn't find any gaps.\nYou have great coverage across categories.",
            color = WornColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun GapsContent(
    state: GapsState,
    onCardClick: (GapRecommendation) -> Unit,
    onBannerClick: () -> Unit,
) {
    GapsBanner(isAiMode = state.isAiMode, onClick = onBannerClick)
    Spacer(Modifier.height(20.dp))

    val grouped = state.recommendations.groupBy { it.category }
    grouped.forEach { (category, items) ->
        SectionLabel(category)
        Spacer(Modifier.height(10.dp))
        items.forEach { recommendation ->
            GapCard(
                recommendation = recommendation,
                isAiMode = state.isAiMode,
                onClick = { onCardClick(recommendation) },
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GapsBanner(isAiMode: Boolean, onClick: () -> Unit) {
    val bgColor = if (isAiMode) WornColors.AccentGreen else WornColors.AccentGreenDark
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAiMode) "AI Recommendations" else "Common Suggestions",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (isAiMode) {
                        "Personalized suggestions based on your wardrobe"
                    } else {
                        "Connect Claude AI for personalized picks"
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = WornColors.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun GapCard(
    recommendation: GapRecommendation,
    isAiMode: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = WornColors.BgCard,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            CategoryIcon(category = recommendation.mappedCategory)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.itemName,
                    color = WornColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (isAiMode) {
                        "Would pair with ${recommendation.pairingCount} of your items"
                    } else {
                        "Common wardrobe essential"
                    },
                    color = WornColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = WornColors.IconMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CategoryIcon(category: Category) {
    val color = category.dotColor()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color),
    ) {
        Icon(
            painter = painterResource(category.iconRes()),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun Category.dotColor(): Color = when (this) {
    Category.TOP -> WornColors.CategoryDotTop
    Category.BOTTOM -> WornColors.CategoryDotBottom
    Category.OUTERWEAR -> WornColors.CategoryDotOuterwear
    Category.SHOES -> WornColors.CategoryDotShoes
    Category.ACCESSORY -> WornColors.CategoryDotAccessory
}

// region Detail Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GapDetailSheet(
    recommendation: GapRecommendation,
    isAiMode: Boolean,
    onAddToWardrobe: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WornColors.BgElevated,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
    ) {
        GapDetailContent(
            recommendation = recommendation,
            isAiMode = isAiMode,
            onAddToWardrobe = onAddToWardrobe,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun GapDetailContent(
    recommendation: GapRecommendation,
    isAiMode: Boolean,
    onAddToWardrobe: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        DetailHeader(recommendation)
        Spacer(Modifier.height(16.dp))
        DetailPairingInfo(recommendation, isAiMode)
        Spacer(Modifier.height(16.dp))
        DetailRows(recommendation)
        Spacer(Modifier.height(24.dp))
        DetailActions(onAddToWardrobe, onDismiss)
    }
}

@Composable
private fun DetailHeader(recommendation: GapRecommendation) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(WornColors.BgCard),
    ) {
        Icon(
            painter = painterResource(recommendation.mappedCategory.iconRes()),
            contentDescription = null,
            tint = WornColors.IconMuted,
            modifier = Modifier.size(48.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = recommendation.itemName,
        color = WornColors.TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(recommendation.mappedCategory.dotColor()),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = recommendation.mappedCategory.displayLabel(),
            color = WornColors.TextSecondary,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun DetailPairingInfo(recommendation: GapRecommendation, isAiMode: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = WornColors.BgCard,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = WornColors.AccentGreen,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isAiMode) {
                    "Would pair with ${recommendation.pairingCount} of your items"
                } else {
                    "Common wardrobe essential"
                },
                color = WornColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun DetailRows(recommendation: GapRecommendation) {
    Column {
        recommendation.subcategory?.let {
            DetailRow("Subcategory", it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase))
        }
        if (recommendation.colors.isNotEmpty()) {
            DetailRow("Color", recommendation.colors.joinToString(", "))
        }
        if (recommendation.seasons.isNotEmpty()) {
            val seasonsText = if (recommendation.seasons.size == 4) "All seasons" else {
                recommendation.seasons.joinToString(", ") {
                    it.name.lowercase().replaceFirstChar(Char::uppercase)
                }
            }
            DetailRow("Season", seasonsText)
        }
        recommendation.fit?.let {
            DetailRow("Fit", it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase))
        }
        recommendation.material?.let {
            DetailRow("Material", it.name.lowercase().replaceFirstChar(Char::uppercase))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = WornColors.TextSecondary, fontSize = 14.sp)
        Text(value, color = WornColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailActions(onAddToWardrobe: () -> Unit, onDismiss: () -> Unit) {
    val gradient = Brush.verticalGradient(
        listOf(WornColors.SaveGradientStart, WornColors.SaveGradientEnd),
    )
    Surface(
        onClick = onAddToWardrobe,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(gradient),
        ) {
            Text(
                "Add to Wardrobe",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Surface(
        onClick = onDismiss,
        shape = RoundedCornerShape(16.dp),
        color = WornColors.BgCard,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(
                "Dismiss",
                color = WornColors.TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// endregion

private fun Category.displayLabel(): String = when (this) {
    Category.TOP -> "Tops"
    Category.BOTTOM -> "Bottoms"
    Category.OUTERWEAR -> "Outerwear"
    Category.SHOES -> "Shoes"
    Category.ACCESSORY -> "Accessories"
}

private fun GapRecommendation.toPreFilledItem() =
    com.github.worn.domain.model.ClothingItem(
        id = "",
        name = itemName,
        category = mappedCategory,
        colors = colors,
        seasons = seasons,
        subcategory = subcategory,
        fit = fit,
        material = material,
        photoPath = "",
        createdAt = 0L,
    )

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun GapsScreenPhonePreview() {
    WornTheme {
        GapsScaffold(
            state = GapsState(
                recommendations = com.github.worn.domain.model.capsuleWardrobeSuggestions.take(6),
                isAiMode = false,
                hasApiKey = false,
            ),
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun GapsScreenTabletPreview() {
    WornTheme {
        GapsScaffold(
            state = GapsState(
                recommendations = com.github.worn.domain.model.capsuleWardrobeSuggestions.take(6),
                isAiMode = true,
                hasApiKey = true,
            ),
            isCompact = false,
        )
    }
}

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun GapsScreenCompletePreview() {
    WornTheme {
        GapsScaffold(state = GapsState())
    }
}
