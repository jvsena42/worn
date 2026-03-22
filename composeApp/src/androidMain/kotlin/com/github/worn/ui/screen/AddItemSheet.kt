package com.github.worn.ui.screen

import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.Season
import com.github.worn.ui.components.AiBadge
import com.github.worn.ui.components.AiLockedSheet
import com.github.worn.ui.components.CategoryDropdown
import com.github.worn.ui.components.ColorSection
import com.github.worn.ui.components.ItemNameField
import com.github.worn.ui.components.PhotoUploadZone
import com.github.worn.ui.components.SaveButton
import com.github.worn.ui.components.SeasonSection
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.WornColors
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    isSaving: Boolean,
    hasApiKey: Boolean,
    onSave: (
        imageBytes: ByteArray, name: String, category: Category,
        colors: List<String>, seasons: List<Season>,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WornColors.BgElevated,
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
        dragHandle = { SheetHandle() },
    ) {
        AddItemForm(isSaving = isSaving, hasApiKey = hasApiKey, onSave = onSave)
    }
}

@Composable
private fun SheetHandle() {
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

@Composable
internal fun AddItemForm(
    isSaving: Boolean = false,
    hasApiKey: Boolean = false,
    onSave: (ByteArray, String, Category, List<String>, List<Season>) -> Unit =
        { _, _, _, _, _ -> },
) {
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var photoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedColors by remember { mutableStateOf(setOf<String>()) }
    var selectedSeasons by remember { mutableStateOf(setOf<Season>()) }
    var showSourceChooser by remember { mutableStateOf(false) }
    var showAiLockedSheet by remember { mutableStateOf(false) }

    PhotoSourceChooser(
        show = showSourceChooser,
        onDismiss = { showSourceChooser = false },
        onPhoto = { bytes, bitmap ->
            photoBytes = bytes
            photoBitmap = bitmap
        },
    )

    if (showAiLockedSheet) {
        AiLockedSheet(onDismiss = { showAiLockedSheet = false })
    }

    AddItemFormContent(
        photoBitmap = photoBitmap,
        name = name,
        onNameChange = { name = it },
        selectedCategory = selectedCategory,
        onCategorySelected = { selectedCategory = it },
        selectedColors = selectedColors,
        onColorToggle = { toggleInSet(it, selectedColors) { selectedColors = it } },
        selectedSeasons = selectedSeasons,
        onSeasonToggle = { toggleInSet(it, selectedSeasons) { selectedSeasons = it } },
        isSaving = isSaving,
        canSave = photoBytes != null && name.isNotBlank() && selectedCategory != null,
        onPhotoClick = { showSourceChooser = true },
        onAiBadgeClick = { if (!hasApiKey) showAiLockedSheet = true },
        onSave = {
            val bytes = photoBytes ?: return@AddItemFormContent
            val cat = selectedCategory ?: return@AddItemFormContent
            onSave(bytes, name, cat, selectedColors.toList(), selectedSeasons.toList())
        },
    )
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
private fun AddItemFormContent(
    photoBitmap: ImageBitmap?,
    name: String,
    onNameChange: (String) -> Unit,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    selectedColors: Set<String>,
    onColorToggle: (String) -> Unit,
    selectedSeasons: Set<Season>,
    onSeasonToggle: (Season) -> Unit,
    isSaving: Boolean,
    canSave: Boolean,
    onPhotoClick: () -> Unit,
    onAiBadgeClick: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Add new item",
            color = WornColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        PhotoUploadZone(bitmap = photoBitmap, onClick = onPhotoClick)
        AiBadge(onClick = onAiBadgeClick)
        ItemNameField(value = name, onValueChange = onNameChange)
        CategoryDropdown(selected = selectedCategory, onSelected = onCategorySelected)
        ColorSection(selectedColors = selectedColors, onToggle = onColorToggle)
        SeasonSection(selectedSeasons = selectedSeasons, onToggle = onSeasonToggle)
        SaveButton(enabled = canSave && !isSaving, isSaving = isSaving, onClick = onSave)
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
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Choose from gallery", fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private inline fun <T> toggleInSet(item: T, current: Set<T>, update: (Set<T>) -> Unit) {
    update(if (item in current) current - item else current + item)
}

private const val JPEG_QUALITY = 90

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun AddItemFormPhonePreview() {
    SheetPreview { AddItemForm() }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun AddItemFormTabletPreview() {
    SheetPreview { AddItemForm() }
}
