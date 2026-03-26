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
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.ui.components.AiBadge
import com.github.worn.ui.components.AiLockedSheet
import com.github.worn.ui.components.CategoryDropdown
import com.github.worn.ui.components.ColorSection
import com.github.worn.ui.components.FitSection
import com.github.worn.ui.components.ItemNameField
import com.github.worn.ui.components.MaterialSection
import com.github.worn.ui.components.PhotoUploadZone
import com.github.worn.ui.components.SaveButton
import com.github.worn.ui.components.SeasonSection
import com.github.worn.ui.components.SubcategoryDropdown
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.WornColors
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    isSaving: Boolean,
    hasApiKey: Boolean,
    existingItem: ClothingItem? = null,
    onSave: (
        imageBytes: ByteArray, name: String, category: Category,
        colors: List<String>, seasons: List<Season>,
        subcategory: Subcategory?, fit: Fit?, material: Material?,
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
        AddItemForm(
            isSaving = isSaving,
            hasApiKey = hasApiKey,
            existingItem = existingItem,
            onSave = onSave,
        )
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
    existingItem: ClothingItem? = null,
    onSave: (ByteArray, String, Category, List<String>, List<Season>, Subcategory?, Fit?, Material?) -> Unit =
        { _, _, _, _, _, _, _, _ -> },
) {
    val formState = rememberAddItemFormState(existingItem)

    PhotoSourceChooser(
        show = formState.showSourceChooser,
        onDismiss = { formState.showSourceChooser = false },
        onPhoto = { bytes, bitmap -> formState.photoBytes = bytes; formState.photoBitmap = bitmap },
    )

    if (formState.showAiLockedSheet) {
        AiLockedSheet(onDismiss = { formState.showAiLockedSheet = false })
    }

    AddItemFormContent(
        photoBitmap = formState.photoBitmap ?: formState.existingPhotoBitmap,
        name = formState.name,
        onNameChange = { formState.name = it },
        selectedCategory = formState.selectedCategory,
        onCategorySelected = { formState.selectedCategory = it; formState.selectedSubcategory = null },
        selectedSubcategory = formState.selectedSubcategory,
        onSubcategorySelected = { formState.selectedSubcategory = it },
        selectedColors = formState.selectedColors,
        onColorToggle = { toggleInSet(it, formState.selectedColors) { formState.selectedColors = it } },
        selectedSeasons = formState.selectedSeasons,
        onSeasonToggle = { toggleInSet(it, formState.selectedSeasons) { formState.selectedSeasons = it } },
        selectedFit = formState.selectedFit,
        onFitSelected = { formState.selectedFit = it },
        selectedMaterial = formState.selectedMaterial,
        onMaterialSelected = { formState.selectedMaterial = it },
        isSaving = isSaving,
        canSave = formState.hasPhoto && formState.name.isNotBlank() && formState.selectedCategory != null,
        isEditing = existingItem != null,
        onPhotoClick = { formState.showSourceChooser = true },
        onAiBadgeClick = { if (!hasApiKey) formState.showAiLockedSheet = true },
        onSave = {
            val cat = formState.selectedCategory ?: return@AddItemFormContent
            val bytes = formState.photoBytes ?: ByteArray(0)
            onSave(bytes, formState.name, cat, formState.selectedColors.toList(),
                formState.selectedSeasons.toList(), formState.selectedSubcategory,
                formState.selectedFit, formState.selectedMaterial)
        },
    )
}

private class AddItemFormState(
    existingItem: ClothingItem?,
    val existingPhotoBitmap: ImageBitmap?,
) {
    var photoBytes by mutableStateOf<ByteArray?>(null)
    var photoBitmap by mutableStateOf<ImageBitmap?>(null)
    var name by mutableStateOf(existingItem?.name ?: "")
    var selectedCategory by mutableStateOf(existingItem?.category)
    var selectedColors by mutableStateOf(existingItem?.colors?.toSet() ?: emptySet())
    var selectedSeasons by mutableStateOf(existingItem?.seasons?.toSet() ?: emptySet())
    var selectedSubcategory by mutableStateOf(existingItem?.subcategory)
    var selectedFit by mutableStateOf(existingItem?.fit)
    var selectedMaterial by mutableStateOf(existingItem?.material)
    var showSourceChooser by mutableStateOf(false)
    var showAiLockedSheet by mutableStateOf(false)
    val hasPhoto: Boolean get() = photoBytes != null || existingPhotoBitmap != null
}

@Composable
private fun rememberAddItemFormState(existingItem: ClothingItem?): AddItemFormState {
    val existingPhotoBitmap = remember(existingItem?.photoPath) {
        existingItem?.photoPath?.takeIf { it.isNotEmpty() }?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
        }
    }
    return remember { AddItemFormState(existingItem, existingPhotoBitmap) }
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
    selectedSubcategory: Subcategory?,
    onSubcategorySelected: (Subcategory) -> Unit,
    selectedColors: Set<String>,
    onColorToggle: (String) -> Unit,
    selectedSeasons: Set<Season>,
    onSeasonToggle: (Season) -> Unit,
    selectedFit: Fit?,
    onFitSelected: (Fit?) -> Unit,
    selectedMaterial: Material?,
    onMaterialSelected: (Material?) -> Unit,
    isSaving: Boolean,
    canSave: Boolean,
    isEditing: Boolean = false,
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
            text = if (isEditing) "Edit item" else "Add new item",
            color = WornColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        PhotoUploadZone(bitmap = photoBitmap, onClick = onPhotoClick)
        if (!isEditing) AiBadge(onClick = onAiBadgeClick)
        ItemNameField(value = name, onValueChange = onNameChange)
        CategoryDropdown(selected = selectedCategory, onSelected = onCategorySelected)
        if (selectedCategory != null) {
            SubcategoryDropdown(
                category = selectedCategory,
                selected = selectedSubcategory,
                onSelected = onSubcategorySelected,
            )
        }
        ColorSection(selectedColors = selectedColors, onToggle = onColorToggle)
        SeasonSection(selectedSeasons = selectedSeasons, onToggle = onSeasonToggle)
        FitSection(selected = selectedFit, onSelected = onFitSelected)
        MaterialSection(selected = selectedMaterial, onSelected = onMaterialSelected)
        SaveButton(
            enabled = canSave && !isSaving,
            isSaving = isSaving,
            onClick = onSave,
            label = if (isEditing) "Save Changes" else null,
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
