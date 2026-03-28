@file:Suppress("TooManyFunctions")

package com.github.worn.ui.screen

import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.compose.AsyncImage
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.TryItResult
import com.github.worn.presentation.viewmodel.TryItEffect
import com.github.worn.presentation.viewmodel.TryItIntent
import com.github.worn.presentation.viewmodel.TryItState
import com.github.worn.presentation.viewmodel.TryItViewModel
import com.github.worn.ui.components.Tab
import com.github.worn.ui.components.WornBottomBar
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme
import org.koin.compose.viewmodel.koinViewModel
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun TryItScreen(onTabSelected: (Tab) -> Unit) {
    val viewModel: TryItViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    var photoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showSourceChooser by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ClothingItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TryItEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    PhotoSourceChooser(
        show = showSourceChooser,
        onDismiss = { showSourceChooser = false },
        onPhoto = { bytes, bitmap ->
            photoBytes = bytes
            photoBitmap = bitmap
            viewModel.onIntent(TryItIntent.Reset)
        },
    )

    TryItScaffold(
        state = state,
        isCompact = isCompact,
        photoBitmap = photoBitmap,
        hasPhoto = photoBytes != null,
        snackbarHostState = snackbarHostState,
        onPhotoClick = { showSourceChooser = true },
        onAnalyze = { photoBytes?.let { viewModel.onIntent(TryItIntent.AnalyzePhoto(it)) } },
        onItemClick = { selectedItem = it },
        onGoToSettings = { onTabSelected(Tab.SETTINGS) },
        onTabSelected = onTabSelected,
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
    onPhoto: (ByteArray, ImageBitmap) -> Unit,
) {
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bmp ->
                    onPhoto(bytes, bmp.asImageBitmap())
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(android.graphics.Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            onPhoto(stream.toByteArray(), it.asImageBitmap())
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
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
        title = { Text("Add photo") },
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
                        Text("Take photo", fontSize = 16.sp)
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
                        Text("Choose from gallery", fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TryItScaffold(
    state: TryItState,
    isCompact: Boolean,
    photoBitmap: ImageBitmap?,
    hasPhoto: Boolean,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onPhotoClick: () -> Unit = {},
    onAnalyze: () -> Unit = {},
    onItemClick: (ClothingItem) -> Unit = {},
    onGoToSettings: () -> Unit = {},
    onTabSelected: (Tab) -> Unit = {},
) {
    val contentPadding = if (isCompact) 24.dp else 32.dp

    Scaffold(
        containerColor = WornColors.BgPage,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            WornBottomBar(
                activeTab = Tab.TRY_IT,
                onTabSelected = onTabSelected,
                isCompact = isCompact,
            )
        },
    ) { paddingValues ->
        if (!state.hasApiKey) {
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
                onPhotoClick = onPhotoClick,
                onAnalyze = onAnalyze,
                onItemClick = onItemClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = contentPadding),
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
    val titleSize = if (isCompact) 24.sp else 26.sp
    val descWidth = if (isCompact) 280.dp else 380.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = WornColors.BgCard,
            border = BorderStroke(1.dp, WornColors.BorderSubtle),
            shadowElevation = 8.dp,
            modifier = Modifier.size(circleSize),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = WornColors.AccentIndigo,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "AI powered analysis",
            color = WornColors.TextPrimary,
            fontSize = titleSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Connect your Claude API key in Settings to analyze items against your wardrobe.",
            color = WornColors.TextSecondary,
            fontSize = if (isCompact) 15.sp else 16.sp,
            lineHeight = if (isCompact) 22.sp else 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = descWidth),
        )
        Spacer(Modifier.height(24.dp))
        IndigoCtaButton(text = "Connect Claude AI", onClick = onGoToSettings)
    }
}

@Composable
private fun IndigoCtaButton(text: String, onClick: () -> Unit) {
    val gradient = Brush.verticalGradient(
        listOf(WornColors.AccentIndigo, Color(0xFF556070)),
    )
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(gradient, RoundedCornerShape(28.dp))
                .padding(horizontal = 40.dp, vertical = 14.dp),
        ) {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TryItContent(
    state: TryItState,
    isCompact: Boolean,
    photoBitmap: ImageBitmap?,
    hasPhoto: Boolean,
    onPhotoClick: () -> Unit,
    onAnalyze: () -> Unit,
    onItemClick: (ClothingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isCompact) {
        TryItPhoneContent(state, photoBitmap, hasPhoto, onPhotoClick, onAnalyze, onItemClick, modifier)
    } else {
        TryItTabletContent(state, photoBitmap, hasPhoto, onPhotoClick, onAnalyze, onItemClick, modifier)
    }
}

@Composable
private fun TryItPhoneContent(
    state: TryItState,
    photoBitmap: ImageBitmap?,
    hasPhoto: Boolean,
    onPhotoClick: () -> Unit,
    onAnalyze: () -> Unit,
    onItemClick: (ClothingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        TryItTitle(fontSize = 28.sp)
        UploadZone(photoBitmap = photoBitmap, height = 200.dp, onClick = onPhotoClick)
        if (hasPhoto && state.result == null && !state.isLoading) {
            AnalyzeButton(onClick = onAnalyze)
        }
        if (state.isLoading) {
            LoadingIndicator()
        }
        state.error?.let { errorMsg ->
            if (!state.isLoading) ErrorMessage(errorMsg)
        }
        state.result?.let { result ->
            ResultsSection(result = result, isCompact = true, onItemClick = onItemClick)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TryItTabletContent(
    state: TryItState,
    photoBitmap: ImageBitmap?,
    hasPhoto: Boolean,
    onPhotoClick: () -> Unit,
    onAnalyze: () -> Unit,
    onItemClick: (ClothingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(4.dp))
        TryItTitle(fontSize = 32.sp)
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
                if (hasPhoto && state.result == null && !state.isLoading) {
                    AnalyzeButton(onClick = onAnalyze)
                }
                if (state.isLoading) {
                    LoadingIndicator()
                }
                state.error?.let { errorMsg ->
                    if (!state.isLoading) ErrorMessage(errorMsg)
                }
                state.result?.let { result ->
                    PairsSection(
                        matchingItems = result.matchingItems,
                        thumbSize = 90.dp,
                        onItemClick = onItemClick,
                    )
                }
            }
            state.result?.let { result ->
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
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TryItTitle(fontSize: androidx.compose.ui.unit.TextUnit) {
    Text(
        text = "Would it fit your wardrobe?",
        color = WornColors.TextPrimary,
        fontSize = fontSize,
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
        shape = RoundedCornerShape(20.dp),
        color = WornColors.BgCard,
        border = BorderStroke(1.5.dp, WornColors.BorderStrong),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().height(height),
    ) {
        if (photoBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = photoBitmap,
                contentDescription = "Selected photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
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
                    tint = WornColors.IconMuted,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Upload a photo of the item\nyou're considering",
                    color = WornColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AnalyzeButton(onClick: () -> Unit) {
    val gradient = Brush.verticalGradient(
        listOf(WornColors.AccentIndigo, Color(0xFF556070)),
    )
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(28.dp))
                .padding(vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "Analyze with Claude",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
    ) {
        CircularProgressIndicator(color = WornColors.AccentIndigo)
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WornColors.DeleteRed.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            color = WornColors.DeleteRed,
            fontSize = 14.sp,
            modifier = Modifier.padding(16.dp),
        )
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
            text = "It would pair with...",
            color = WornColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
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
        shape = RoundedCornerShape(16.dp),
        color = WornColors.BgCard,
        border = BorderStroke(1.dp, WornColors.BorderSubtle),
        shadowElevation = 2.dp,
        modifier = Modifier.size(size),
    ) {
        if (item.photoPath.isNotEmpty() && File(item.photoPath).exists()) {
            AsyncImage(
                model = File(item.photoPath),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = item.name,
                    tint = WornColors.TextSecondary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun CombinationsCard(count: Int, isCompact: Boolean) {
    val cardHeight = if (isCompact) 90.dp else 110.dp
    val valueSize = if (isCompact) 40.sp else 44.sp

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WornColors.BgCard,
        border = BorderStroke(1.dp, WornColors.BorderSubtle),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().height(cardHeight),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Combinations unlocked",
                color = WornColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = count.toString(),
                color = WornColors.AccentGreen,
                fontSize = valueSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.2).sp,
            )
        }
    }
}

@Composable
private fun GapsFilledSection(gaps: List<String>, isCompact: Boolean) {
    if (gaps.isEmpty()) return

    val fontSize = if (isCompact) 14.sp else 15.sp

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Wardrobe gaps it fills",
            color = WornColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
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
                        .background(WornColors.AccentGreen),
                )
                Text(
                    text = gap,
                    color = WornColors.TextPrimary,
                    fontSize = fontSize,
                )
            }
        }
    }
}

@Composable
private fun DecisionBanner(worthAdding: Boolean, isCompact: Boolean) {
    val bannerHeight = if (isCompact) 56.dp else 60.dp
    val gradient = if (worthAdding) {
        Brush.verticalGradient(listOf(WornColors.AccentGreen, WornColors.AccentGreenDark))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF8B7D7D), Color(0xFF6B5E5E)))
    }
    val icon = if (worthAdding) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel
    val text = if (worthAdding) "Worth adding" else "Skip this one"

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .clip(RoundedCornerShape(28.dp))
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
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private const val JPEG_QUALITY = 90

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

@Preview(showSystemUi = true, device = "id:pixel_8")
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

@Preview(showSystemUi = true, device = "id:pixel_8")
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

@Preview(showSystemUi = true, device = "id:pixel_tablet")
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

// endregion
