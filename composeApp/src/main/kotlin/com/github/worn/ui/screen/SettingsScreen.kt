@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

@file:Suppress("TooManyFunctions")

package com.github.worn.ui.screen

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.R
import com.github.worn.domain.model.AgeRange
import com.github.worn.domain.model.BodyType
import com.github.worn.domain.model.Climate
import com.github.worn.domain.model.Lifestyle
import com.github.worn.domain.model.OnDeviceAiAvailability
import com.github.worn.domain.model.OnDeviceAiUnavailableReason
import com.github.worn.domain.model.StyleProfile
import com.github.worn.domain.model.UserProfile
import com.github.worn.domain.model.isUsable
import com.github.worn.presentation.viewmodel.SettingsEffect
import com.github.worn.presentation.viewmodel.SettingsIntent
import com.github.worn.presentation.viewmodel.SettingsState
import com.github.worn.presentation.viewmodel.SettingsViewModel
import com.github.worn.ui.components.SheetDragHandle
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornChip
import com.github.worn.ui.components.WornGradientButton
import com.github.worn.ui.components.WornTopAppBarTitlePadding
import com.github.worn.ui.components.wornTopAppBarColors
import com.github.worn.ui.exposeTestTagsAsResourceId
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornTheme
import com.github.worn.ui.theme.sheetShape
import com.github.worn.ui.theme.wornExtras
import org.koin.compose.viewmodel.koinViewModel

@Suppress("UnusedParameter")
@Composable
fun SettingsScreen(onTabSelected: (Tab) -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    var showProfileSheet by remember { mutableStateOf(false) }
    var showApiKeySheet by remember { mutableStateOf(false) }
    var showYouCamSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            if (effect is SettingsEffect.YouCamCredentialsSaved) {
                showYouCamSheet = false
            }
        }
    }

    SettingsScaffold(
        state = state,
        isCompact = isCompact,
        onProfileClick = { showProfileSheet = true },
        onApiKeyClick = { showApiKeySheet = true },
        onYouCamClick = { showYouCamSheet = true },
        onOnDeviceAiChange = { viewModel.onIntent(SettingsIntent.SetOnDeviceAi(it)) },
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

    if (showYouCamSheet) {
        YouCamCredentialsSheet(
            hasCredentials = state.hasYouCamKey,
            verifying = state.verifyingYouCam,
            errorMessage = state.youCamError,
            onSave = { clientId, clientSecret ->
                viewModel.onIntent(SettingsIntent.SaveYouCamCredentials(clientId, clientSecret))
            },
            onClear = {
                viewModel.onIntent(SettingsIntent.ClearYouCamCredentials)
                showYouCamSheet = false
            },
            onDismiss = { showYouCamSheet = false },
        )
    }
}

@Composable
private fun SettingsScaffold(
    state: SettingsState,
    isCompact: Boolean = true,
    onProfileClick: () -> Unit = {},
    onApiKeyClick: () -> Unit = {},
    onYouCamClick: () -> Unit = {},
    onOnDeviceAiChange: (Boolean) -> Unit = {},
) {
    val contentPadding = if (isCompact) 24.dp else 32.dp

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .testTag("settings_screen")
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        modifier = WornTopAppBarTitlePadding,
                    )
                },
                colors = wornTopAppBarColors(),
                scrollBehavior = scrollBehavior,
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
            Spacer(Modifier.height(8.dp))
            SectionLabel(stringResource(R.string.settings_section_profile))
            Spacer(Modifier.height(10.dp))
            SettingsCard(
                icon = { SettingsIcon(color = MaterialTheme.colorScheme.primary, icon = Icons.Outlined.Person) },
                title = stringResource(R.string.settings_your_profile),
                subtitle = state.userProfile.summaryText(),
                onClick = onProfileClick,
                modifier = Modifier.testTag("settings_profile_card"),
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_section_ai))
            Spacer(Modifier.height(10.dp))
            // Listed before the key card: it is free and private, so it should be the first option.
            SettingsToggleCard(
                icon = { SettingsIcon(color = MaterialTheme.colorScheme.primary, icon = Icons.Outlined.PhoneAndroid) },
                title = stringResource(R.string.settings_on_device_ai_title),
                subtitle = stringResource(state.onDeviceAiAvailability.subtitleRes()),
                checked = state.onDeviceAiEnabled,
                enabled = state.onDeviceAiAvailability.isUsable,
                onCheckedChange = onOnDeviceAiChange,
                modifier = Modifier.testTag("settings_on_device_ai_toggle"),
            )
            Spacer(Modifier.height(10.dp))
            SettingsCard(
                icon = { SettingsIcon(color = MaterialTheme.colorScheme.secondary, icon = Icons.Outlined.AutoAwesome) },
                title = stringResource(R.string.settings_api_key_title),
                subtitle = stringResource(
                    if (state.hasApiKey) R.string.settings_api_key_connected else R.string.settings_api_key_required,
                ),
                onClick = onApiKeyClick,
                modifier = Modifier.testTag("settings_api_key_card"),
            )
            Spacer(Modifier.height(10.dp))
            SettingsCard(
                icon = { SettingsIcon(color = MaterialTheme.colorScheme.secondary, icon = Icons.Outlined.Checkroom) },
                title = stringResource(R.string.settings_youcam_title),
                subtitle = stringResource(
                    if (state.hasYouCamKey) R.string.settings_youcam_connected else R.string.settings_youcam_required,
                ),
                onClick = onYouCamClick,
                modifier = Modifier.testTag("settings_youcam_card"),
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_section_about))
            Spacer(Modifier.height(10.dp))
            AboutCard()

            Spacer(Modifier.height(24.dp))
            DonationCard()

        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun SettingsIcon(color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.medium)
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

/**
 * A [SettingsCard] whose trailing affordance is a switch instead of a chevron. The whole row is
 * tappable so the target matches the other cards, and the switch is disabled — rather than hidden —
 * when the capability is missing, so the subtitle can explain why.
 */
@Composable
private fun SettingsToggleCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            icon()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@StringRes
private fun OnDeviceAiAvailability.subtitleRes(): Int = when (this) {
    OnDeviceAiAvailability.Available -> R.string.settings_on_device_ai_available
    OnDeviceAiAvailability.Downloadable -> R.string.settings_on_device_ai_downloading
    is OnDeviceAiAvailability.Unavailable -> when (reason) {
        OnDeviceAiUnavailableReason.UNSUPPORTED_DEVICE ->
            R.string.settings_on_device_ai_unsupported_device
        OnDeviceAiUnavailableReason.UNSUPPORTED_OS ->
            R.string.settings_on_device_ai_unsupported_os
        OnDeviceAiUnavailableReason.DISABLED_BY_USER ->
            R.string.settings_on_device_ai_disabled_by_user
        OnDeviceAiUnavailableReason.UNKNOWN -> R.string.settings_on_device_ai_unavailable
    }
}

@Composable
private fun SettingsCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            icon()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.wornExtras.iconMuted,
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
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.settings_version),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    versionName ?: "1.0",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            AboutLinkRow(stringResource(R.string.settings_suggestions_bugs)) { uriHandler.openUri(FEEDBACK_URL) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            AboutLinkRow(stringResource(R.string.settings_licenses)) { uriHandler.openUri(LICENSE_URL) }
        }
    }
}

@Composable
private fun AboutLinkRow(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.wornExtras.iconMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DonationCard() {
    val clipboardManager = LocalClipboardManager.current
    val copiedText = stringResource(R.string.settings_donate_copied)
    var showCopied by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.settings_donate_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_donate_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = {
                    clipboardManager.setText(AnnotatedString(DONATION_LN_ADDRESS))
                    showCopied = true
                },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                ) {
                    Text(
                        DONATION_LN_ADDRESS,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (showCopied) copiedText else stringResource(R.string.settings_donate_copy),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

// region Sheet Handle


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
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.sheetShape,
        dragHandle = { SheetDragHandle() },
    ) {
        ProfileSheetContent(state = state, onIntent = onIntent, onSave = onSave)
    }
}

@Composable
private fun ProfileSheetContent(state: SettingsState, onIntent: (SettingsIntent) -> Unit, onSave: () -> Unit) {
    // displayName() reads a string resource, so it stays in composition; the lists are not.
    val bodyTypeLabels = BodyType.entries.map { it.displayName() }
    val styleProfileLabels = StyleProfile.entries.map { it.displayName() }
    val ageRangeLabels = AgeRange.entries.map { it.displayName() }
    val climateLabels = Climate.entries.map { it.displayName() }
    val lifestyleLabels = Lifestyle.entries.map { it.displayName() }

    val bodyTypeOptions = remember(bodyTypeLabels) { BodyType.entries.zip(bodyTypeLabels) }
    val styleProfileOptions = remember(styleProfileLabels) { StyleProfile.entries.zip(styleProfileLabels) }
    val ageRangeOptions = remember(ageRangeLabels) { AgeRange.entries.zip(ageRangeLabels) }
    val climateOptions = remember(climateLabels) { Climate.entries.zip(climateLabels) }
    val lifestyleOptions = remember(lifestyleLabels) { Lifestyle.entries.zip(lifestyleLabels) }

    Column(
        modifier = Modifier
            .exposeTestTagsAsResourceId()
            .testTag("profile_sheet")
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_your_profile),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.settings_profile_help),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
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
        SaveGradientButton(
            text = stringResource(R.string.common_save),
            onClick = onSave,
            modifier = Modifier.testTag("profile_save_button"),
        )
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.sheetShape,
        dragHandle = { SheetDragHandle() },
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
            .exposeTestTagsAsResourceId()
            .testTag("api_key_sheet")
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
            modifier = Modifier.testTag("api_key_field"),
        )
        SaveGradientButton(
            text = stringResource(R.string.settings_save_connect),
            enabled = !hasApiKey && keyInput.isNotBlank(),
            onClick = {
                onSave(keyInput)
                keyInput = ""
            },
            modifier = Modifier.testTag("api_key_save_button"),
        )
        if (hasApiKey) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = onClear,
                    color = Color.Transparent,
                    modifier = Modifier.testTag("api_key_remove_button"),
                ) {
                    Text(
                        text = stringResource(R.string.settings_remove_key),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
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
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        text = stringResource(R.string.settings_api_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = stringResource(R.string.settings_api_get_key),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun ApiKeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
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
                    tint = MaterialTheme.wornExtras.iconMuted,
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
}

// endregion

// region YouCam Credentials Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YouCamCredentialsSheet(
    hasCredentials: Boolean,
    verifying: Boolean,
    errorMessage: String?,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.sheetShape,
        dragHandle = { SheetDragHandle() },
    ) {
        YouCamCredentialsSheetContent(
            hasCredentials = hasCredentials,
            verifying = verifying,
            errorMessage = errorMessage,
            onSave = onSave,
            onClear = onClear,
        )
    }
}

@Composable
private fun YouCamCredentialsSheetContent(
    hasCredentials: Boolean,
    verifying: Boolean,
    errorMessage: String?,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
) {
    var clientId by remember { mutableStateOf("") }
    var clientSecret by remember { mutableStateOf("") }
    var idVisible by remember { mutableStateOf(false) }
    var secretVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .exposeTestTagsAsResourceId()
            .testTag("youcam_sheet")
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_youcam_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.settings_youcam_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(R.string.settings_youcam_get_key),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        FieldLabel(stringResource(R.string.settings_youcam_client_id_hint))
        ApiKeyTextField(
            value = if (hasCredentials) "••••••••••••••••" else clientId,
            onValueChange = { if (!hasCredentials) clientId = it },
            enabled = !hasCredentials,
            passwordVisible = idVisible,
            onToggleVisibility = { idVisible = !idVisible },
            modifier = Modifier.testTag("youcam_client_id_field"),
        )
        FieldLabel(stringResource(R.string.settings_youcam_client_secret_hint))
        ApiKeyTextField(
            value = if (hasCredentials) "••••••••••••••••" else clientSecret,
            onValueChange = { if (!hasCredentials) clientSecret = it },
            enabled = !hasCredentials,
            passwordVisible = secretVisible,
            onToggleVisibility = { secretVisible = !secretVisible },
            modifier = Modifier.testTag("youcam_client_secret_field"),
        )
        SaveGradientButton(
            text = stringResource(
                if (verifying) R.string.settings_youcam_verifying else R.string.settings_youcam_save,
            ),
            enabled = !hasCredentials && !verifying && clientId.isNotBlank() && clientSecret.isNotBlank(),
            onClick = { onSave(clientId, clientSecret) },
            modifier = Modifier.testTag("youcam_save_button"),
        )
        if (errorMessage != null && !verifying) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("youcam_error"),
            )
        }
        if (hasCredentials) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = onClear,
                    color = Color.Transparent,
                    modifier = Modifier.testTag("youcam_remove_button"),
                ) {
                    Text(
                        text = stringResource(R.string.settings_youcam_remove),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

// endregion

// region Shared components

@Composable
private fun SaveGradientButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WornGradientButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
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
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, label) ->
                WornChip(
                    label = label,
                    isActive = value == selected,
                    onClick = { onSelected(if (value == selected) null else value) },
                )
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
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.settings_multi_select),
                color = MaterialTheme.wornExtras.textMuted,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, label) ->
                WornChip(
                    label = label,
                    isActive = value in selected,
                    onClick = { onToggle(value) },
                )
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

private const val DONATION_LN_ADDRESS = "jvsena42@blink.sv"
private const val FEEDBACK_URL = "https://github.com/jvsena42/worn/issues/new"
private const val LICENSE_URL = "https://github.com/jvsena42/worn/blob/main/LICENSE"

@PhonePreview
@Composable
private fun SettingsScreenPhonePreview() {
    WornTheme {
        SettingsScaffold(
            state = SettingsState(
                onDeviceAiEnabled = true,
                onDeviceAiAvailability = OnDeviceAiAvailability.Available,
            ),
        )
    }
}

@TabletPreview
@Composable
private fun SettingsScreenTabletPreview() {
    WornTheme {
        SettingsScaffold(
            state = SettingsState(
                onDeviceAiEnabled = true,
                onDeviceAiAvailability = OnDeviceAiAvailability.Available,
            ),
            isCompact = false,
        )
    }
}

/** The default [SettingsState] already reports on-device AI as unavailable. */
@PhonePreview
@Composable
private fun SettingsScreenOnDeviceAiUnavailablePreview() {
    WornTheme {
        SettingsScaffold(state = SettingsState())
    }
}



