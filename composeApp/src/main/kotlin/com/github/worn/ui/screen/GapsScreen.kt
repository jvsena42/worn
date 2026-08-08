@file:Suppress("TooManyFunctions")

package com.github.worn.ui.screen

import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.R
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.GapRecommendation
import com.github.worn.domain.model.Season
import com.github.worn.presentation.viewmodel.GapsEffect
import com.github.worn.presentation.viewmodel.GapsIntent
import com.github.worn.presentation.viewmodel.GapsState
import com.github.worn.presentation.viewmodel.GapsViewModel
import com.github.worn.ui.components.AiLockedSheet
import com.github.worn.ui.components.ErrorContentView
import com.github.worn.ui.components.SheetDragHandle
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornGradientButton
import com.github.worn.ui.components.displayLabel
import com.github.worn.ui.components.displayName
import com.github.worn.ui.components.iconRes
import com.github.worn.ui.exposeTestTagsAsResourceId
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornDimens
import com.github.worn.ui.theme.WornTheme
import com.github.worn.ui.theme.wornExtras
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GapsScreen(onTabSelected: (Tab) -> Unit) {
    val viewModel: GapsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    var selectedGap by remember { mutableStateOf<GapRecommendation?>(null) }
    var showAiLockedSheet by remember { mutableStateOf(false) }
    var showAddItemSheet by remember { mutableStateOf(false) }
    var addItemPreFill by remember { mutableStateOf<GapRecommendation?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GapsEffect.ItemAdded -> {
                    showAddItemSheet = false
                    addItemPreFill = null
                }
                is GapsEffect.ShowError ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    GapsScaffold(
        state = state,
        isCompact = isCompact,
        onRetry = { viewModel.onIntent(GapsIntent.LoadGaps) },
        onCardClick = { selectedGap = it },
        onBannerClick = { if (!state.isAiMode) showAiLockedSheet = true },
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
            isSaving = state.isSaving,
            isAiAvailable = state.isAiAvailable,
            prefillItem = gap.toPreFilledItem(),
            onSave = { imageBytes, name, category, colors, seasons, subcategory, fit, material ->
                viewModel.onIntent(
                    GapsIntent.AddItem(
                        imageBytes = imageBytes,
                        name = name,
                        category = category,
                        colors = colors,
                        seasons = seasons,
                        subcategory = subcategory,
                        fit = fit,
                        material = material,
                    ),
                )
            },
            onDismiss = {
                showAddItemSheet = false
                addItemPreFill = null
            },
        )
    }
}

@Composable
private fun GapsScaffold(
    state: GapsState,
    isCompact: Boolean = true,
    onRetry: () -> Unit = {},
    onCardClick: (GapRecommendation) -> Unit = {},
    onBannerClick: () -> Unit = {},
) {
    val contentPadding = if (isCompact) 24.dp else 32.dp

    Scaffold(
        modifier = Modifier.testTag("gaps_screen"),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = contentPadding),
        ) {
            item(key = "header") {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.gaps_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = stringResource(R.string.gaps_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(20.dp))
            }

            when {
                state.isLoading -> item(key = "loading") { LoadingContent() }
                state.error != null -> item(key = "error") {
                    ErrorContentView(
                        message = state.error!!,
                        onRetry = onRetry,
                        modifier = Modifier.padding(vertical = 60.dp),
                    )
                }
                state.recommendations.isEmpty() -> item(key = "complete") { CompleteContent() }
                else -> gapsContent(
                    state = state,
                    onCardClick = onCardClick,
                    onBannerClick = onBannerClick,
                )
            }

            item(key = "bottom_clearance") {
                Spacer(Modifier.height(WornDimens.BottomBarClearance))
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.gaps_complete_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.gaps_complete_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 20.sp,
        )
    }
}

/** Grouping runs here while building the item list, not in a composable body. */
private fun LazyListScope.gapsContent(
    state: GapsState,
    onCardClick: (GapRecommendation) -> Unit,
    onBannerClick: () -> Unit,
) {
    item(key = "banner") {
        GapsBanner(isAiMode = state.isAiMode, onClick = onBannerClick)
        Spacer(Modifier.height(20.dp))
    }

    state.recommendations.groupBy { it.category }.forEach { (category, items) ->
        item(key = "section_$category") {
            SectionLabel(category)
            Spacer(Modifier.height(10.dp))
        }
        items(items, key = { "${category}_${it.itemName}" }) { recommendation ->
            GapCard(
                recommendation = recommendation,
                isAiMode = state.isAiMode,
                onClick = { onCardClick(recommendation) },
            )
            Spacer(Modifier.height(8.dp))
        }
        item(key = "section_gap_$category") { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun GapsBanner(isAiMode: Boolean, onClick: () -> Unit) {
    val bgColor = if (isAiMode) MaterialTheme.colorScheme.primary else MaterialTheme.wornExtras.accentGreenDark
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = Modifier.testTag("gaps_banner"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val titleRes = if (isAiMode) R.string.gaps_banner_ai_title
                    else R.string.gaps_banner_common_title
                val subtitleRes = if (isAiMode) R.string.gaps_banner_ai_subtitle
                    else R.string.gaps_banner_common_subtitle
                Text(
                    text = stringResource(titleRes),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(subtitleRes),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge,
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
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
        color = MaterialTheme.colorScheme.surfaceContainer,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (isAiMode) {
                        stringResource(R.string.gaps_pairing_ai, recommendation.pairingCount)
                    } else {
                        stringResource(R.string.gaps_pairing_common)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.wornExtras.iconMuted,
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

@Composable
@ReadOnlyComposable
private fun Category.dotColor(): Color = when (this) {
    Category.TOP -> MaterialTheme.wornExtras.categoryDotTop
    Category.BOTTOM -> MaterialTheme.wornExtras.categoryDotBottom
    Category.OUTERWEAR -> MaterialTheme.wornExtras.categoryDotOuterwear
    Category.SHOES -> MaterialTheme.wornExtras.categoryDotShoes
    Category.ACCESSORY -> MaterialTheme.wornExtras.categoryDotAccessory
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { SheetDragHandle(color = MaterialTheme.colorScheme.outline) },
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
            .exposeTestTagsAsResourceId()
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
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Icon(
            painter = painterResource(recommendation.mappedCategory.iconRes()),
            contentDescription = null,
            tint = MaterialTheme.wornExtras.iconMuted,
            modifier = Modifier.size(48.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = recommendation.itemName,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DetailPairingInfo(recommendation: GapRecommendation, isAiMode: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isAiMode) {
                    stringResource(R.string.gaps_pairing_ai, recommendation.pairingCount)
                } else {
                    stringResource(R.string.gaps_pairing_common)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun DetailRows(recommendation: GapRecommendation) {
    Column {
        recommendation.subcategory?.let {
            DetailRow(stringResource(R.string.label_subcategory), it.displayName())
        }
        if (recommendation.colors.isNotEmpty()) {
            DetailRow(stringResource(R.string.label_color), recommendation.colors.joinToString(", "))
        }
        if (recommendation.seasons.isNotEmpty()) {
            val seasonsText = if (recommendation.seasons.size == 4) {
                stringResource(R.string.common_all_seasons)
            } else {
                val context = LocalContext.current
                recommendation.seasons.joinToString(", ") { season ->
                    context.getString(season.stringRes())
                }
            }
            DetailRow(stringResource(R.string.label_season), seasonsText)
        }
        recommendation.fit?.let {
            DetailRow(stringResource(R.string.label_fit), it.displayName())
        }
        recommendation.material?.let {
            DetailRow(stringResource(R.string.label_material), it.displayName())
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DetailActions(onAddToWardrobe: () -> Unit, onDismiss: () -> Unit) {
    WornGradientButton(
        text = stringResource(R.string.gaps_add_to_wardrobe),
        onClick = onAddToWardrobe,
        modifier = Modifier.testTag("gap_add_to_wardrobe"),
    )
    Spacer(Modifier.height(8.dp))
    Surface(
        onClick = onDismiss,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().testTag("gap_dismiss"),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(
                stringResource(R.string.gaps_dismiss),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// endregion

@StringRes
private fun Season.stringRes(): Int = when (this) {
    Season.SPRING -> R.string.season_spring
    Season.SUMMER -> R.string.season_summer
    Season.FALL -> R.string.season_fall
    Season.WINTER -> R.string.season_winter
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

@PhonePreview
@Composable
private fun GapsScreenPhonePreview() {
    WornTheme {
        GapsScaffold(
            state = GapsState(
                recommendations = com.github.worn.domain.model.capsuleWardrobeSuggestions.take(6),
                isAiMode = false,
                isAiAvailable = false,
            ),
        )
    }
}

@TabletPreview
@Composable
private fun GapsScreenTabletPreview() {
    WornTheme {
        GapsScaffold(
            state = GapsState(
                recommendations = com.github.worn.domain.model.capsuleWardrobeSuggestions.take(6),
                isAiMode = true,
                isAiAvailable = true,
            ),
            isCompact = false,
        )
    }
}

@PhonePreview
@Composable
private fun GapsScreenCompletePreview() {
    WornTheme {
        GapsScaffold(state = GapsState())
    }
}

@PhonePreview
@Composable
private fun GapsScreenErrorPreview() {
    WornTheme {
        GapsScaffold(
            state = GapsState(
                error = "Invalid API key. Check your key in Settings.",
                isAiMode = true,
                isAiAvailable = true,
            ),
        )
    }
}

