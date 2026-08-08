@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("TooManyFunctions")

package com.github.worn.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.github.worn.R
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.GarmentCategory
import com.github.worn.domain.model.TryItFeature
import com.github.worn.domain.model.TryItResult
import com.github.worn.presentation.viewmodel.TryItEffect
import com.github.worn.presentation.viewmodel.TryItIntent
import com.github.worn.presentation.viewmodel.TryItState
import com.github.worn.presentation.viewmodel.TryItViewModel
import com.github.worn.ui.components.ClothingPhoto
import com.github.worn.ui.components.CropEditorDialog
import com.github.worn.ui.components.CropPhotoButton
import com.github.worn.ui.components.ErrorContentView
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornGradientButton
import com.github.worn.ui.components.WornGradients
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornDimens
import com.github.worn.ui.theme.WornTheme
import com.github.worn.ui.theme.wornExtras
import com.github.worn.ui.util.SharedPhoto
import com.github.worn.ui.util.readImageBytes
import com.github.worn.ui.util.rememberCameraCapture
import com.github.worn.ui.util.rememberDecodedImage
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TryItScreen(
    onTabSelected: (Tab) -> Unit,
    sharedPhoto: SharedPhoto? = null,
    onSharedPhotoConsumed: () -> Unit = {},
) {
    val viewModel: TryItViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    val photoBitmap = rememberDecodedImage(photoBytes)
    var showSourceChooser by remember { mutableStateOf(false) }
    var showPersonSourceChooser by remember { mutableStateOf(false) }
    var showCropEditor by remember { mutableStateOf(false) }
    var showPersonCropEditor by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ClothingItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var tryOnSectionTop by remember { mutableIntStateOf(0) }
    var viewportTop by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TryItEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SharedPhotoEffect(
        sharedPhoto = sharedPhoto,
        snackbarHostState = snackbarHostState,
        onBytes = { bytes ->
            photoBytes = bytes
            viewModel.onIntent(TryItIntent.Reset)
            viewModel.onIntent(TryItIntent.ReceiveSharedPhoto)
        },
        onConsumed = onSharedPhotoConsumed,
    )

    FeatureFocusEffect(
        focusedFeature = state.focusedFeature,
        scrollState = scrollState,
        tryOnSectionTop = { tryOnSectionTop },
        viewportTop = { viewportTop },
        onFocusHandled = { viewModel.onIntent(TryItIntent.ClearFeatureFocus) },
    )

    if (state.featureChoiceRequired) {
        FeatureChooserDialog(
            onDismiss = { viewModel.onIntent(TryItIntent.ClearFeatureFocus) },
            onChoose = { viewModel.onIntent(TryItIntent.ChooseFeature(it)) },
        )
    }

    PhotoSourceChooser(
        show = showSourceChooser,
        onDismiss = { showSourceChooser = false },
        onPhoto = { bytes ->
            photoBytes = bytes
            viewModel.onIntent(TryItIntent.Reset)
        },
    )

    PhotoSourceChooser(
        show = showPersonSourceChooser,
        onDismiss = { showPersonSourceChooser = false },
        onPhoto = { bytes -> viewModel.onIntent(TryItIntent.SetPersonPhoto(bytes)) },
    )

    if (showCropEditor) {
        photoBytes?.let { bytes ->
            CropEditorDialog(
                imageBytes = bytes,
                onCancel = { showCropEditor = false },
                onCropped = { cropped ->
                    photoBytes = cropped
                    // The previous analysis described the uncropped photo.
                    viewModel.onIntent(TryItIntent.Reset)
                    showCropEditor = false
                },
            )
        }
    }

    if (showPersonCropEditor) {
        state.personImage?.let { bytes ->
            CropEditorDialog(
                imageBytes = bytes,
                onCancel = { showPersonCropEditor = false },
                onCropped = { cropped ->
                    // Round-tripping through the intent also persists the cropped model photo.
                    viewModel.onIntent(TryItIntent.SetPersonPhoto(cropped))
                    showPersonCropEditor = false
                },
            )
        }
    }

    TryItScaffold(
        state = state,
        isCompact = isCompact,
        photoBitmap = photoBitmap,
        hasPhoto = photoBytes != null,
        snackbarHostState = snackbarHostState,
        scrollState = scrollState,
        onTryOnSectionPositioned = { tryOnSectionTop = it },
        onScrollViewportPositioned = { viewportTop = it },
        onPhotoClick = { showSourceChooser = true },
        onCropClick = { showCropEditor = true },
        onAnalyze = { photoBytes?.let { viewModel.onIntent(TryItIntent.AnalyzePhoto(it)) } },
        onSelectCategory = { viewModel.onIntent(TryItIntent.SelectCategory(it)) },
        onPersonPhotoClick = { showPersonSourceChooser = true },
        onPersonCropClick = { showPersonCropEditor = true },
        onGenerateTryOn = { photoBytes?.let { viewModel.onIntent(TryItIntent.GenerateTryOn(it)) } },
        onItemClick = { selectedItem = it },
        onGoToSettings = { onTabSelected(Tab.SETTINGS) },
    )

    if (selectedItem != null) {
        ItemDetailSheet(
            item = selectedItem!!,
            isCompact = isCompact,
            onDismiss = { selectedItem = null },
            showActions = false,
        )
    }
}

@Composable
private fun PhotoSourceChooser(
    show: Boolean,
    onDismiss: () -> Unit,
    onPhoto: (ByteArray) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            scope.launch { readImageBytes(context, it)?.let(onPhoto) }
        }
    }

    val takePicture = rememberCameraCapture(onPhoto)

    val cameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) takePicture()
    }

    if (show) {
        PhotoSourceDialog(
            onDismiss = onDismiss,
            onCamera = {
                onDismiss()
                cameraPermission.launch(Manifest.permission.CAMERA)
            },
            onGallery = {
                onDismiss()
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_item_photo_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.add_item_take_photo), style = MaterialTheme.typography.titleSmall)
                    }
                }
                TextButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.add_item_choose_gallery),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/**
 * Places a photo arriving from the share sheet, or reports that its URI could not be read.
 *
 * [onConsumed] clears the share so it is handled exactly once: the pager disposes this screen when
 * it is more than one page away, and without this the share would replay on every return to the tab.
 */
@Composable
private fun SharedPhotoEffect(
    sharedPhoto: SharedPhoto?,
    snackbarHostState: SnackbarHostState,
    onBytes: (ByteArray) -> Unit,
    onConsumed: () -> Unit,
) {
    val readFailedMessage = stringResource(R.string.share_photo_read_failed)
    LaunchedEffect(sharedPhoto) {
        val photo = sharedPhoto ?: return@LaunchedEffect
        photo.bytes?.let(onBytes)
        // Before the snackbar, which suspends until dismissed: a dispose while it is showing
        // would otherwise leave the share unconsumed and replay it.
        onConsumed()
        if (photo.bytes == null) snackbarHostState.showSnackbar(readFailedMessage)
    }
}

/** Scrolls the section a shared photo was routed to into view, then releases the focus. */
@Composable
private fun FeatureFocusEffect(
    focusedFeature: TryItFeature?,
    scrollState: ScrollState,
    tryOnSectionTop: () -> Int,
    viewportTop: () -> Int,
    onFocusHandled: () -> Unit,
) {
    LaunchedEffect(focusedFeature) {
        when (focusedFeature) {
            TryItFeature.ANALYSIS -> scrollState.animateScrollTo(0)
            TryItFeature.VIRTUAL_TRY_ON -> {
                // The positions are reported during layout, which can land after this effect
                // starts; one frame of slack means we measure against a placed section.
                withFrameNanos { }
                // Both are root-relative, so adding the current scroll turns the on-screen delta
                // back into an absolute offset within the scrollable content.
                val target = tryOnSectionTop() - viewportTop() + scrollState.value
                scrollState.animateScrollTo(target.coerceIn(0, scrollState.maxValue))
            }
            null -> return@LaunchedEffect
        }
        onFocusHandled()
    }
}

/** Asked only when both credentials are connected, so a shared photo is ambiguous. */
@Composable
private fun FeatureChooserDialog(
    onDismiss: () -> Unit,
    onChoose: (TryItFeature) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("share_feature_chooser"),
        title = { Text(stringResource(R.string.share_choose_feature_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FeatureChoiceRow(
                    icon = Icons.Outlined.SmartToy,
                    label = stringResource(R.string.share_choose_analyze),
                    testTag = "share_choose_analyze",
                    onClick = { onChoose(TryItFeature.ANALYSIS) },
                )
                FeatureChoiceRow(
                    icon = Icons.Outlined.AutoAwesome,
                    label = stringResource(R.string.share_choose_try_on),
                    testTag = "share_choose_try_on",
                    onClick = { onChoose(TryItFeature.VIRTUAL_TRY_ON) },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun FeatureChoiceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag(testTag)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun TryItScaffold(
    state: TryItState,
    isCompact: Boolean,
    photoBitmap: ImageBitmap?,
    hasPhoto: Boolean,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    scrollState: ScrollState = rememberScrollState(),
    onTryOnSectionPositioned: (Int) -> Unit = {},
    onScrollViewportPositioned: (Int) -> Unit = {},
    onPhotoClick: () -> Unit = {},
    onCropClick: () -> Unit = {},
    onAnalyze: () -> Unit = {},
    onSelectCategory: (GarmentCategory) -> Unit = {},
    onPersonPhotoClick: () -> Unit = {},
    onPersonCropClick: () -> Unit = {},
    onGenerateTryOn: () -> Unit = {},
    onItemClick: (ClothingItem) -> Unit = {},
    onGoToSettings: () -> Unit = {},
) {
    val contentPadding = if (isCompact) 24.dp else 32.dp

    Scaffold(
        modifier = Modifier.testTag("try_it_screen"),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (!state.hasApiKey && !state.hasYouCamKey) {
            AiEmptyContent(
                isCompact = isCompact,
                onGoToSettings = onGoToSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = contentPadding),
            )
        } else {
            TryItContent(
                state = state,
                isCompact = isCompact,
                photoBitmap = photoBitmap,
                hasPhoto = hasPhoto,
                scrollState = scrollState,
                onTryOnSectionPositioned = onTryOnSectionPositioned,
                onPhotoClick = onPhotoClick,
                onCropClick = onCropClick,
                onAnalyze = onAnalyze,
                onSelectCategory = onSelectCategory,
                onPersonPhotoClick = onPersonPhotoClick,
                onPersonCropClick = onPersonCropClick,
                onGenerateTryOn = onGenerateTryOn,
                onItemClick = onItemClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = contentPadding)
                    // Before verticalScroll in the chain, so this reports the viewport, which
                    // stays put while the content inside it translates.
                    .onGloballyPositioned { onScrollViewportPositioned(it.positionInRoot().y.toInt()) },
            )
        }
    }
}

@Composable
private fun AiEmptyContent(
    isCompact: Boolean,
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val circleSize = if (isCompact) 130.dp else 150.dp
    val iconSize = if (isCompact) 52.dp else 60.dp
    val titleStyle = if (isCompact) {
        MaterialTheme.typography.headlineSmall
    } else {
        MaterialTheme.typography.headlineLarge
    }
    val descWidth = if (isCompact) 280.dp else 380.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 8.dp,
            modifier = Modifier.size(circleSize),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.tryit_locked_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = titleStyle,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.tryit_locked_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = if (isCompact) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            lineHeight = if (isCompact) 22.sp else 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = descWidth),
        )
        Spacer(Modifier.height(24.dp))
        IndigoCtaButton(
            text = stringResource(R.string.tryit_open_settings),
            onClick = onGoToSettings,
            modifier = Modifier.testTag("try_it_connect_cta"),
        )
    }
}

@Composable
private fun IndigoCtaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    WornGradientButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        gradientColors = WornGradients.Indigo,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        elevation = 6.dp,
        fillMaxWidth = false,
        fixedHeight = null,
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 14.dp),
    )
}

@Composable
private fun TryItContent(
    state: TryItState,
    isCompact: Boolean,
    photoBitmap: ImageBitmap?,
    hasPhoto: Boolean,
    scrollState: ScrollState,
    onTryOnSectionPositioned: (Int) -> Unit,
    onPhotoClick: () -> Unit,
    onCropClick: () -> Unit,
    onAnalyze: () -> Unit,
    onSelectCategory: (GarmentCategory) -> Unit,
    onPersonPhotoClick: () -> Unit,
    onPersonCropClick: () -> Unit,
    onGenerateTryOn: () -> Unit,
    onItemClick: (ClothingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isCompact) {
        TryItPhoneContent(
            state = state,
            photoBitmap = photoBitmap,
            hasPhoto = hasPhoto,
            scrollState = scrollState,
            onTryOnSectionPositioned = onTryOnSectionPositioned,
            onPhotoClick = onPhotoClick,
            onCropClick = onCropClick,
            onAnalyze = onAnalyze,
            onSelectCategory = onSelectCategory,
            onPersonPhotoClick = onPersonPhotoClick,
            onPersonCropClick = onPersonCropClick,
            onGenerateTryOn = onGenerateTryOn,
            onItemClick = onItemClick,
            modifier = modifier,
        )
    } else {
        TryItTabletContent(
            state = state,
            photoBitmap = photoBitmap,
            hasPhoto = hasPhoto,
            scrollState = scrollState,
            onTryOnSectionPositioned = onTryOnSectionPositioned,
            onPhotoClick = onPhotoClick,
            onCropClick = onCropClick,
            onAnalyze = onAnalyze,
            onSelectCategory = onSelectCategory,
            onPersonPhotoClick = onPersonPhotoClick,
            onPersonCropClick = onPersonCropClick,
            onGenerateTryOn = onGenerateTryOn,
            onItemClick = onItemClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun TryItPhoneContent(
    state: TryItState,
    photoBitmap: ImageBitmap?,
    hasPhoto: Boolean,
    scrollState: ScrollState,
    onTryOnSectionPositioned: (Int) -> Unit,
    onPhotoClick: () -> Unit,
    onCropClick: () -> Unit,
    onAnalyze: () -> Unit,
    onSelectCategory: (GarmentCategory) -> Unit,
    onPersonPhotoClick: () -> Unit,
    onPersonCropClick: () -> Unit,
    onGenerateTryOn: () -> Unit,
    onItemClick: (ClothingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        TryItTitle(style = MaterialTheme.typography.headlineMedium)
        UploadZone(photoBitmap = photoBitmap, height = 200.dp, onClick = onPhotoClick)
        if (hasPhoto) {
            CropPhotoButton(onClick = onCropClick, modifier = Modifier.testTag("try_it_crop_button"))
        }
        if (state.hasApiKey) {
            if (hasPhoto && state.result == null && !state.isLoading) {
                AnalyzeButton(onClick = onAnalyze)
            }
            if (state.isLoading) {
                LoadingIndicator()
            }
            state.error?.let { errorMsg ->
                if (!state.isLoading) {
                    ErrorContentView(
                        message = errorMsg,
                        onRetry = onAnalyze,
                        modifier = Modifier.padding(vertical = 40.dp),
                        retryButtonColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            state.result?.let { result ->
                ResultsSection(result = result, isCompact = true, onItemClick = onItemClick)
            }
        }
        if (state.hasYouCamKey) {
            TryOnSection(
                state = state,
                hasPhoto = hasPhoto,
                isCompact = true,
                onSelectCategory = onSelectCategory,
                onPersonPhotoClick = onPersonPhotoClick,
                onPersonCropClick = onPersonCropClick,
                onGenerateTryOn = onGenerateTryOn,
                onPositioned = onTryOnSectionPositioned,
            )
        }
        Spacer(Modifier.height(WornDimens.BottomBarClearance))
    }
}

@Composable
private fun TryItTabletContent(
    state: TryItState,
    photoBitmap: ImageBitmap?,
    hasPhoto: Boolean,
    scrollState: ScrollState,
    onTryOnSectionPositioned: (Int) -> Unit,
    onPhotoClick: () -> Unit,
    onCropClick: () -> Unit,
    onAnalyze: () -> Unit,
    onSelectCategory: (GarmentCategory) -> Unit,
    onPersonPhotoClick: () -> Unit,
    onPersonCropClick: () -> Unit,
    onGenerateTryOn: () -> Unit,
    onItemClick: (ClothingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(scrollState)) {
        Spacer(Modifier.height(4.dp))
        TryItTitle(style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(28.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.weight(1f),
            ) {
                UploadZone(photoBitmap = photoBitmap, height = 300.dp, onClick = onPhotoClick)
                if (hasPhoto) {
                    CropPhotoButton(onClick = onCropClick, modifier = Modifier.testTag("try_it_crop_button"))
                }
                if (state.hasApiKey) {
                    if (hasPhoto && state.result == null && !state.isLoading) {
                        AnalyzeButton(onClick = onAnalyze)
                    }
                    if (state.isLoading) {
                        LoadingIndicator()
                    }
                    state.error?.let { errorMsg ->
                        if (!state.isLoading) {
                            ErrorContentView(
                                message = errorMsg,
                                onRetry = onAnalyze,
                                modifier = Modifier.padding(vertical = 40.dp),
                                retryButtonColor = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    state.result?.let { result ->
                        PairsSection(
                            matchingItems = result.matchingItems,
                            thumbSize = 90.dp,
                            onItemClick = onItemClick,
                        )
                    }
                }
                if (state.hasYouCamKey) {
                    TryOnSection(
                        state = state,
                        hasPhoto = hasPhoto,
                        isCompact = false,
                        onSelectCategory = onSelectCategory,
                        onPersonPhotoClick = onPersonPhotoClick,
                        onPersonCropClick = onPersonCropClick,
                        onGenerateTryOn = onGenerateTryOn,
                        onPositioned = onTryOnSectionPositioned,
                    )
                }
            }
            state.result?.let { result ->
                if (state.hasApiKey) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        CombinationsCard(count = result.combinationsUnlocked, isCompact = false)
                        GapsFilledSection(gaps = result.gapsFilled, isCompact = false)
                        DecisionBanner(worthAdding = result.worthAdding, isCompact = false)
                    }
                }
            }
        }
        Spacer(Modifier.height(WornDimens.BottomBarClearance))
    }
}

@Composable
private fun TryItTitle(style: androidx.compose.ui.text.TextStyle) {
    Text(
        text = stringResource(R.string.tryit_title),
        color = MaterialTheme.colorScheme.onSurface,
        style = style,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.8).sp,
    )
}

@Composable
private fun UploadZone(
    photoBitmap: ImageBitmap?,
    height: Dp,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().height(height).testTag("try_it_upload_zone"),
    ) {
        if (photoBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = photoBitmap,
                contentDescription = "Selected photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.largeIncreased),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.wornExtras.iconMuted,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.tryit_upload_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AnalyzeButton(onClick: () -> Unit) {
    WornGradientButton(
        text = stringResource(R.string.tryit_analyze),
        onClick = onClick,
        modifier = Modifier.testTag("try_it_analyze_button"),
        gradientColors = WornGradients.Indigo,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        elevation = 6.dp,
        fixedHeight = null,
        contentPadding = PaddingValues(vertical = 14.dp),
        icon = {
            Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        },
    )
}

@Composable
private fun LoadingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun ResultsSection(
    result: TryItResult,
    isCompact: Boolean,
    onItemClick: (ClothingItem) -> Unit,
) {
    val thumbSize = if (isCompact) 80.dp else 90.dp
    PairsSection(matchingItems = result.matchingItems, thumbSize = thumbSize, onItemClick = onItemClick)
    CombinationsCard(count = result.combinationsUnlocked, isCompact = isCompact)
    GapsFilledSection(gaps = result.gapsFilled, isCompact = isCompact)
    DecisionBanner(worthAdding = result.worthAdding, isCompact = isCompact)
}

@Composable
private fun PairsSection(
    matchingItems: List<ClothingItem>,
    thumbSize: Dp,
    onItemClick: (ClothingItem) -> Unit,
) {
    if (matchingItems.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.tryit_pairs_with),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = (-0.2).sp,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(matchingItems, key = { it.id }) { item ->
                ItemThumbnail(
                    item = item,
                    size = thumbSize,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun ItemThumbnail(item: ClothingItem, size: Dp, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        modifier = Modifier.size(size),
    ) {
        ClothingPhoto(
            photoPath = item.photoPath,
            contentDescription = item.name,
            shape = MaterialTheme.shapes.large,
            placeholderIconSize = 28.dp,
            placeholderTint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CombinationsCard(count: Int, isCompact: Boolean) {
    val cardHeight = if (isCompact) 90.dp else 110.dp
    val valueStyle = MaterialTheme.typography.displaySmall

    Surface(
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .heightIn(min = cardHeight)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.tryit_combinations_unlocked),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = count.toString(),
                color = MaterialTheme.colorScheme.primary,
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.2).sp,
            )
        }
    }
}

@Composable
private fun GapsFilledSection(gaps: List<String>, isCompact: Boolean) {
    if (gaps.isEmpty()) return

    val bodyStyle = if (isCompact) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyMedium
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.tryit_gaps_filled),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = (-0.2).sp,
        )
        gaps.forEach { gap ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text(
                    text = gap,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = bodyStyle,
                )
            }
        }
    }
}

@Composable
private fun DecisionBanner(worthAdding: Boolean, isCompact: Boolean) {
    val bannerHeight = if (isCompact) 56.dp else 60.dp
    val gradient = if (worthAdding) {
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.wornExtras.accentGreenDark))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF8B7D7D), Color(0xFF6B5E5E)))
    }
    val icon = if (worthAdding) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel
    val text = stringResource(if (worthAdding) R.string.tryit_worth_adding else R.string.tryit_skip)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .clip(MaterialTheme.shapes.extraLargeIncreased)
            .background(gradient),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun TryOnSection(
    state: TryItState,
    hasPhoto: Boolean,
    isCompact: Boolean,
    onSelectCategory: (GarmentCategory) -> Unit,
    onPersonPhotoClick: () -> Unit,
    onPersonCropClick: () -> Unit,
    onGenerateTryOn: () -> Unit,
    onPositioned: (Int) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .testTag("try_on_section")
            // Root-relative, not parent-relative: on tablet this sits inside a nested column.
            .onGloballyPositioned { onPositioned(it.positionInRoot().y.toInt()) },
    ) {
        Text(
            text = stringResource(R.string.tryit_your_photo_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = (-0.2).sp,
        )
        PersonPhotoZone(
            personImage = state.personImage,
            height = if (isCompact) 200.dp else 260.dp,
            onClick = onPersonPhotoClick,
        )
        if (state.personImage != null) {
            CropPhotoButton(
                onClick = onPersonCropClick,
                modifier = Modifier.testTag("try_on_person_crop_button"),
            )
        }
        Text(
            text = stringResource(R.string.tryit_tryon_category_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = (-0.2).sp,
        )
        GarmentCategorySelector(selected = state.selectedCategory, onSelect = onSelectCategory)
        val readyToGenerate = hasPhoto && state.selectedCategory != null && state.personImage != null
        val idle = state.tryOnImage == null && !state.tryOnLoading
        if (readyToGenerate && idle) {
            SeeItOnMeButton(onClick = onGenerateTryOn)
        }
        if (state.tryOnLoading) {
            TryOnLoadingIndicator()
        }
        state.tryOnError?.let { errorMsg ->
            if (!state.tryOnLoading) {
                ErrorContentView(
                    message = errorMsg,
                    onRetry = onGenerateTryOn,
                    modifier = Modifier.padding(vertical = 24.dp),
                    retryButtonColor = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        state.tryOnImage?.let { image ->
            TryOnResultView(imageBytes = image, height = if (isCompact) 320.dp else 400.dp)
        }
        Text(
            text = stringResource(R.string.tryit_tryon_cost_note),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun PersonPhotoZone(personImage: ByteArray?, height: Dp, onClick: () -> Unit) {
    val bitmap = rememberDecodedImage(personImage)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().height(height).testTag("try_on_person_zone"),
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.tryit_your_photo_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.largeIncreased),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.wornExtras.iconMuted,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.tryit_your_photo_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GarmentCategorySelector(
    selected: GarmentCategory?,
    onSelect: (GarmentCategory) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("try_on_category_selector"),
    ) {
        GarmentCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(stringResource(categoryLabelRes(category))) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

private fun categoryLabelRes(category: GarmentCategory): Int = when (category) {
    GarmentCategory.TOP -> R.string.tryit_category_top
    GarmentCategory.BOTTOM -> R.string.tryit_category_bottom
    GarmentCategory.FULL_BODY -> R.string.tryit_category_full
    GarmentCategory.SHOES -> R.string.tryit_category_shoes
}

@Composable
private fun SeeItOnMeButton(onClick: () -> Unit) {
    WornGradientButton(
        text = stringResource(R.string.tryit_see_on_me),
        onClick = onClick,
        modifier = Modifier.testTag("try_on_generate_button"),
        gradientColors = WornGradients.Indigo,
        shape = MaterialTheme.shapes.extraLargeIncreased,
        elevation = 6.dp,
        fixedHeight = null,
        contentPadding = PaddingValues(vertical = 14.dp),
        icon = {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        },
    )
}

@Composable
private fun TryOnLoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        Text(
            text = stringResource(R.string.tryit_tryon_generating),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TryOnResultView(imageBytes: ByteArray, height: Dp) {
    val bitmap = rememberDecodedImage(imageBytes)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.tryit_tryon_result_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = (-0.2).sp,
        )
        Surface(
            shape = MaterialTheme.shapes.largeIncreased,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().height(height).testTag("try_on_result"),
        ) {
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.tryit_tryon_result_title),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.largeIncreased),
                )
            }
        }
    }
}


// region Previews

private val previewResult = TryItResult(
    matchingItems = listOf(
        ClothingItem(
            id = "1", name = "White T-Shirt", category = com.github.worn.domain.model.Category.TOP,
            colors = listOf("White"), seasons = listOf(com.github.worn.domain.model.Season.SPRING),
            photoPath = "", createdAt = 0,
        ),
        ClothingItem(
            id = "2", name = "Blue Jeans", category = com.github.worn.domain.model.Category.BOTTOM,
            colors = listOf("Navy"), seasons = listOf(com.github.worn.domain.model.Season.FALL),
            photoPath = "", createdAt = 0,
        ),
        ClothingItem(
            id = "3", name = "Sneakers", category = com.github.worn.domain.model.Category.SHOES,
            colors = listOf("White"), seasons = listOf(com.github.worn.domain.model.Season.SPRING),
            photoPath = "", createdAt = 0,
        ),
    ),
    combinationsUnlocked = 24,
    gapsFilled = listOf("Neutral layering piece", "Transitional outerwear", "Versatile neutral bottom"),
    worthAdding = true,
)

@PhonePreview
@Composable
private fun TryItResultsPhonePreview() {
    WornTheme {
        TryItScaffold(
            state = TryItState(hasApiKey = true, result = previewResult),
            isCompact = true,
            photoBitmap = null,
            hasPhoto = true,
        )
    }
}

@PhonePreview
@Composable
private fun TryItEmptyPhonePreview() {
    WornTheme {
        TryItScaffold(
            state = TryItState(hasApiKey = false),
            isCompact = true,
            photoBitmap = null,
            hasPhoto = false,
        )
    }
}

@TabletPreview
@Composable
private fun TryItResultsTabletPreview() {
    WornTheme {
        TryItScaffold(
            state = TryItState(hasApiKey = true, result = previewResult),
            isCompact = false,
            photoBitmap = null,
            hasPhoto = true,
        )
    }
}

@PhonePreview
@Composable
private fun TryItShareChooserPhonePreview() {
    WornTheme {
        TryItScaffold(
            state = TryItState(hasApiKey = true, hasYouCamKey = true),
            isCompact = true,
            photoBitmap = null,
            hasPhoto = true,
        )
        FeatureChooserDialog(onDismiss = {}, onChoose = {})
    }
}

@TabletPreview
@Composable
private fun TryItShareChooserTabletPreview() {
    WornTheme {
        TryItScaffold(
            state = TryItState(hasApiKey = true, hasYouCamKey = true),
            isCompact = false,
            photoBitmap = null,
            hasPhoto = true,
        )
        FeatureChooserDialog(onDismiss = {}, onChoose = {})
    }
}

@PhonePreview
@Composable
private fun TryItTryOnPhonePreview() {
    WornTheme {
        TryItScaffold(
            state = TryItState(
                hasYouCamKey = true,
                selectedCategory = GarmentCategory.TOP,
            ),
            isCompact = true,
            photoBitmap = null,
            hasPhoto = true,
        )
    }
}

// endregion

