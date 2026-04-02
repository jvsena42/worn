@file:Suppress("TooManyFunctions")

package com.github.worn.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.R
import com.github.worn.domain.model.AgeRange
import com.github.worn.domain.model.BodyType
import com.github.worn.domain.model.Climate
import com.github.worn.domain.model.Lifestyle
import com.github.worn.domain.model.StyleProfile
import com.github.worn.domain.model.UserProfile
import com.github.worn.presentation.viewmodel.SettingsIntent
import com.github.worn.presentation.viewmodel.SettingsState
import com.github.worn.presentation.viewmodel.SettingsViewModel
import com.github.worn.ui.components.Tab
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme
import org.koin.compose.viewmodel.koinViewModel

@Suppress("UnusedParameter")
@Composable
fun SettingsScreen(onTabSelected: (Tab) -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    var showProfileSheet by remember { mutableStateOf(false) }
    var showApiKeySheet by remember { mutableStateOf(false) }

    SettingsScaffold(
        state = state,
        isCompact = isCompact,
        onProfileClick = { showProfileSheet = true },
        onApiKeyClick = { showApiKeySheet = true },
    )

    if (showProfileSheet) {
        ProfileSheet(
            state = state,
            onIntent = viewModel::onIntent,
            onDismiss = { showProfileSheet = false },
            onSave = { showProfileSheet = false },
        )
    }

    if (showApiKeySheet) {
        ApiKeySheet(
            hasApiKey = state.hasApiKey,
            onSave = {
                viewModel.onIntent(SettingsIntent.SaveApiKey(it))
                showApiKeySheet = false
            },
            onClear = {
                viewModel.onIntent(SettingsIntent.ClearApiKey)
                showApiKeySheet = false
            },
            onDismiss = { showApiKeySheet = false },
        )
    }
}

@Composable
private fun SettingsScaffold(
    state: SettingsState,
    isCompact: Boolean = true,
    onProfileClick: () -> Unit = {},
    onApiKeyClick: () -> Unit = {},
) {
    val contentPadding = if (isCompact) 24.dp else 32.dp

    Scaffold(
        containerColor = WornColors.BgPage,
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
                text = stringResource(R.string.settings_title),
                color = WornColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.settings_section_profile))
            Spacer(Modifier.height(10.dp))
            SettingsCard(
                icon = { SettingsIcon(color = WornColors.AccentGreen, icon = Icons.Outlined.Person) },
                title = stringResource(R.string.settings_your_profile),
                subtitle = state.userProfile.summaryText(),
                onClick = onProfileClick,
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_section_ai))
            Spacer(Modifier.height(10.dp))
            SettingsCard(
                icon = { SettingsIcon(color = WornColors.AccentIndigo, icon = Icons.Outlined.AutoAwesome) },
                title = stringResource(R.string.settings_api_key_title),
                subtitle = stringResource(
                    if (state.hasApiKey) R.string.settings_api_key_connected else R.string.settings_api_key_required,
                ),
                onClick = onApiKeyClick,
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_section_about))
            Spacer(Modifier.height(10.dp))
            AboutCard()

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = WornColors.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun SettingsIcon(color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SettingsCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = WornColors.BgCard,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            icon()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = WornColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = WornColors.TextSecondary, fontSize = 13.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = WornColors.IconMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("1.0")
    }
    val uriHandler = LocalUriHandler.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = WornColors.BgCard,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.settings_version),
                    color = WornColors.TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(versionName ?: "1.0", color = WornColors.TextSecondary, fontSize = 15.sp)
            }
            HorizontalDivider(color = WornColors.BorderSubtle.copy(alpha = 0.5f))
            Surface(
                onClick = { uriHandler.openUri(LICENSE_URL) },
                color = Color.Transparent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(
                        stringResource(R.string.settings_licenses),
                        color = WornColors.TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = WornColors.IconMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// region Sheet Handle

@Composable
private fun SettingsSheetHandle() {
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

// endregion

// region Profile Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSheet(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WornColors.BgElevated,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { SettingsSheetHandle() },
    ) {
        ProfileSheetContent(state = state, onIntent = onIntent, onSave = onSave)
    }
}

@Composable
private fun ProfileSheetContent(state: SettingsState, onIntent: (SettingsIntent) -> Unit, onSave: () -> Unit) {
    val bodyTypeOptions = BodyType.entries.map { it to it.displayName() }
    val styleProfileOptions = StyleProfile.entries.map { it to it.displayName() }
    val ageRangeOptions = AgeRange.entries.map { it to it.displayName() }
    val climateOptions = Climate.entries.map { it to it.displayName() }
    val lifestyleOptions = Lifestyle.entries.map { it to it.displayName() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_your_profile),
            color = WornColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.settings_profile_help),
            color = WornColors.TextSecondary,
            fontSize = 14.sp,
        )
        ChipGroup(
            title = stringResource(R.string.label_body_type),
            options = bodyTypeOptions,
            selected = state.userProfile.bodyType,
            onSelected = { onIntent(SettingsIntent.SelectBodyType(it)) },
        )
        ChipGroup(
            title = stringResource(R.string.label_style_profile),
            options = styleProfileOptions,
            selected = state.userProfile.styleProfile,
            onSelected = { onIntent(SettingsIntent.SelectStyleProfile(it)) },
        )
        ChipGroup(
            title = stringResource(R.string.label_age_range),
            options = ageRangeOptions,
            selected = state.userProfile.ageRange,
            onSelected = { onIntent(SettingsIntent.SelectAgeRange(it)) },
        )
        ChipGroup(
            title = stringResource(R.string.label_climate),
            options = climateOptions,
            selected = state.userProfile.climate,
            onSelected = { onIntent(SettingsIntent.SelectClimate(it)) },
        )
        MultiChipGroup(
            title = stringResource(R.string.label_lifestyle),
            options = lifestyleOptions,
            selected = state.userProfile.lifestyles,
            onToggle = { onIntent(SettingsIntent.ToggleLifestyle(it)) },
        )
        SaveGradientButton(text = stringResource(R.string.common_save), onClick = onSave)
    }
}

// endregion

// region API Key Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeySheet(
    hasApiKey: Boolean,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WornColors.BgElevated,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { SettingsSheetHandle() },
    ) {
        ApiKeySheetContent(hasApiKey = hasApiKey, onSave = onSave, onClear = onClear)
    }
}

@Composable
private fun ApiKeySheetContent(
    hasApiKey: Boolean,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var keyInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ApiKeySheetHeader()
        ApiKeyTextField(
            value = if (hasApiKey) "••••••••••••••••" else keyInput,
            onValueChange = { if (!hasApiKey) keyInput = it },
            enabled = !hasApiKey,
            passwordVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible },
        )
        SaveGradientButton(
            text = stringResource(R.string.settings_save_connect),
            enabled = !hasApiKey && keyInput.isNotBlank(),
            onClick = {
                onSave(keyInput)
                keyInput = ""
            },
        )
        if (hasApiKey) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Surface(onClick = onClear, color = Color.Transparent) {
                    Text(
                        text = stringResource(R.string.settings_remove_key),
                        color = WornColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeySheetHeader() {
    Text(
        text = stringResource(R.string.settings_connect_claude),
        color = WornColors.TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = stringResource(R.string.settings_api_description),
        color = WornColors.TextSecondary,
        fontSize = 14.sp,
    )
    Text(
        text = stringResource(R.string.settings_api_get_key),
        color = WornColors.AccentGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ApiKeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
) {
    val transformation = if (passwordVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        visualTransformation = transformation,
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Outlined.Visibility
                    } else {
                        Icons.Outlined.VisibilityOff
                    },
                    contentDescription = stringResource(
                        if (passwordVisible) R.string.settings_api_hide else R.string.settings_api_show,
                    ),
                    tint = WornColors.IconMuted,
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = WornColors.BgCard,
            unfocusedContainerColor = WornColors.BgCard,
            disabledContainerColor = WornColors.BgCard,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

// endregion

// region Shared components

@Composable
private fun SaveGradientButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(WornColors.SaveGradientStart, WornColors.SaveGradientEnd))
    val disabledGradient = Brush.verticalGradient(listOf(WornColors.TextMuted, WornColors.IconMuted))
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(if (enabled) gradient else disabledGradient),
        ) {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(
    title: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelected: (T?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = WornColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, label) ->
                val isActive = value == selected
                Surface(
                    onClick = { onSelected(if (isActive) null else value) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isActive) WornColors.AccentGreen else WornColors.BgCard,
                    border = if (isActive) null else BorderStroke(1.dp, WornColors.BorderSubtle),
                ) {
                    Text(
                        text = label,
                        color = if (isActive) WornColors.TextOnColor else WornColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiChipGroup(
    title: String,
    options: List<Pair<T, String>>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row {
            Text(title, color = WornColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.settings_multi_select), color = WornColors.TextMuted, fontSize = 12.sp)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, label) ->
                val isActive = value in selected
                Surface(
                    onClick = { onToggle(value) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isActive) WornColors.AccentGreen else WornColors.BgCard,
                    border = if (isActive) null else BorderStroke(1.dp, WornColors.BorderSubtle),
                ) {
                    Text(
                        text = label,
                        color = if (isActive) WornColors.TextOnColor else WornColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

// endregion

// region Display names

@Composable
private fun UserProfile.summaryText(): String {
    val parts = listOfNotNull(
        bodyType?.displayName(),
        styleProfile?.displayName(),
        ageRange?.displayName(),
    )
    return if (parts.isEmpty()) stringResource(R.string.settings_profile_subtitle_empty) else parts.joinToString(" · ")
}

@Composable
private fun BodyType.displayName(): String = when (this) {
    BodyType.SLIM -> stringResource(R.string.body_type_slim)
    BodyType.ATHLETIC -> stringResource(R.string.body_type_athletic)
    BodyType.AVERAGE -> stringResource(R.string.body_type_average)
    BodyType.STOCKY -> stringResource(R.string.body_type_stocky)
    BodyType.SHORT -> stringResource(R.string.body_type_short)
    BodyType.TALL_AND_SLIM -> stringResource(R.string.body_type_tall_and_slim)
    BodyType.TALL_AND_FIT -> stringResource(R.string.body_type_tall_and_fit)
    BodyType.BIG_AND_TALL -> stringResource(R.string.body_type_big_and_tall)
}

@Composable
private fun StyleProfile.displayName(): String = when (this) {
    StyleProfile.CLASSIC -> stringResource(R.string.style_classic)
    StyleProfile.CASUAL -> stringResource(R.string.style_casual)
    StyleProfile.STREETWEAR -> stringResource(R.string.style_streetwear)
    StyleProfile.SMART_CASUAL -> stringResource(R.string.style_smart_casual)
    StyleProfile.MINIMALIST -> stringResource(R.string.style_minimalist)
}

@Composable
private fun AgeRange.displayName(): String = when (this) {
    AgeRange.AGE_18_25 -> stringResource(R.string.age_18_25)
    AgeRange.AGE_26_35 -> stringResource(R.string.age_26_35)
    AgeRange.AGE_36_45 -> stringResource(R.string.age_36_45)
    AgeRange.AGE_46_PLUS -> stringResource(R.string.age_46_plus)
}

@Composable
private fun Climate.displayName(): String = when (this) {
    Climate.TROPICAL -> stringResource(R.string.climate_tropical)
    Climate.TEMPERATE -> stringResource(R.string.climate_temperate)
    Climate.COLD -> stringResource(R.string.climate_cold)
    Climate.MIXED -> stringResource(R.string.climate_mixed)
}

@Composable
private fun Lifestyle.displayName(): String = when (this) {
    Lifestyle.WORK_OFFICE -> stringResource(R.string.lifestyle_work_office)
    Lifestyle.WORK_MANUAL -> stringResource(R.string.lifestyle_work_manual)
    Lifestyle.SOCIAL -> stringResource(R.string.lifestyle_social)
    Lifestyle.SPORTS -> stringResource(R.string.lifestyle_sports)
    Lifestyle.FORMAL_EVENTS -> stringResource(R.string.lifestyle_formal_events)
}

// endregion

private const val LICENSE_URL = "https://github.com/jvsena42/worn/blob/main/LICENSE"

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun SettingsScreenPhonePreview() {
    WornTheme {
        SettingsScaffold(state = SettingsState())
    }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun SettingsScreenTabletPreview() {
    WornTheme {
        SettingsScaffold(state = SettingsState(), isCompact = false)
    }
}
