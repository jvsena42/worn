package com.github.worn.ui.screen

import android.Manifest
import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.worn.R
import com.github.worn.data.source.image.BackgroundRemover
import com.github.worn.domain.model.Category
import com.github.worn.domain.model.ClothingItem
import com.github.worn.domain.model.Fit
import com.github.worn.domain.model.Material
import com.github.worn.domain.model.Season
import com.github.worn.domain.model.Subcategory
import com.github.worn.ui.exposeTestTagsAsResourceId
import com.github.worn.ui.components.AiBadge
import com.github.worn.ui.components.SheetDragHandle
import com.github.worn.ui.components.AiLockedSheet
import com.github.worn.ui.components.CategoryDropdown
import com.github.worn.ui.components.ColorSection
import com.github.worn.ui.components.CropEditorDialog
import com.github.worn.ui.components.CropPhotoButton
import com.github.worn.ui.components.FitSection
import com.github.worn.ui.components.ItemNameField
import com.github.worn.ui.components.MaterialSection
import com.github.worn.ui.components.PhotoUploadZone
import com.github.worn.ui.components.RemoveBackgroundToggle
import com.github.worn.ui.components.SaveButton
import com.github.worn.ui.components.SeasonSection
import com.github.worn.ui.components.SubcategoryDropdown
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.util.decodePreviewImage
import com.github.worn.ui.util.readImageBytes
import com.github.worn.ui.util.rememberCameraCapture
import com.github.worn.ui.util.rememberDecodedImage
import com.github.worn.ui.theme.WornColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    isSaving: Boolean,
    isAiAvailable: Boolean,
    existingItem: ClothingItem? = null,
    prefillItem: ClothingItem? = null,
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
        dragHandle = { SheetDragHandle() },
    ) {
        AddItemForm(
            isSaving = isSaving,
            isAiAvailable = isAiAvailable,
            existingItem = existingItem,
            prefillItem = prefillItem,
            onSave = onSave,
        )
    }
}

@Composable
internal fun AddItemForm(
    isSaving: Boolean = false,
    isAiAvailable: Boolean = false,
    existingItem: ClothingItem? = null,
    /**
     * Seed values for a *new* item, e.g. a Gaps suggestion. Unlike [existingItem] it does not put
     * the sheet in editing mode: a photo is still required and the button still says "Save to
     * wardrobe", because nothing has been stored yet.
     */
    prefillItem: ClothingItem? = null,
    onSave: (ByteArray, String, Category, List<String>, List<Season>, Subcategory?, Fit?, Material?) -> Unit =
        { _, _, _, _, _, _, _, _ -> },
) {
    val formState = rememberAddItemFormState(existingItem, prefillItem)
    val backgroundRemover = koinInject<BackgroundRemover>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    PhotoSourceChooser(
        show = formState.showSourceChooser,
        onDismiss = { formState.showSourceChooser = false },
        onPhoto = { bytes, bitmap ->
            formState.photoBytes = bytes
            formState.originalPhotoBytes = bytes
            formState.photoBitmap = bitmap
            formState.bgRemoved = false
        },
    )

    if (formState.showCropEditor) {
        formState.photoBytes?.let { bytes ->
            CropEditorDialog(
                imageBytes = bytes,
                onCancel = { formState.showCropEditor = false },
                onCropped = { cropped ->
                    formState.photoBytes = cropped
                    // Rebase the background-removal baseline so toggling it off reverts to the
                    // cropped photo rather than resurrecting the pre-crop frame.
                    formState.originalPhotoBytes = cropped
                    formState.bgRemoved = false
                    formState.showCropEditor = false
                    scope.launch { formState.photoBitmap = decodePreviewImage(cropped) }
                },
            )
        }
    }

    if (formState.showAiLockedSheet) {
        AiLockedSheet(onDismiss = { formState.showAiLockedSheet = false })
    }

    fun onRemoveBackgroundChange(enabled: Boolean) {
        val original = formState.originalPhotoBytes ?: return
        if (!enabled) {
            formState.photoBytes = original
            formState.bgRemoved = false
            scope.launch { formState.photoBitmap = decodePreviewImage(original) }
            return
        }
        formState.isProcessingBg = true
        scope.launch {
            runCatching { backgroundRemover.removeBackground(original) }
                .onSuccess { processed ->
                    formState.photoBytes = processed
                    decodePreviewImage(processed)?.let { formState.photoBitmap = it }
                    formState.bgRemoved = true
                }
                .onFailure {
                    formState.bgRemoved = false
                    Toast.makeText(
                        context,
                        context.getString(R.string.add_item_bg_removal_failed),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            formState.isProcessingBg = false
        }
    }

    AddItemFormContent(
        photoBitmap = formState.photoBitmap ?: formState.existingPhotoBitmap,
        canCrop = formState.photoBytes != null,
        onCropClick = { formState.showCropEditor = true },
        canRemoveBackground = formState.photoBytes != null,
        bgRemoved = formState.bgRemoved,
        isProcessingBg = formState.isProcessingBg,
        onRemoveBackgroundChange = ::onRemoveBackgroundChange,
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
        onAiBadgeClick = { if (!isAiAvailable) formState.showAiLockedSheet = true },
        onSave = {
            val cat = formState.selectedCategory ?: return@AddItemFormContent
            val bytes = formState.photoBytes ?: ByteArray(0)
            onSave(bytes, formState.name, cat, formState.selectedColors.toList(),
                formState.selectedSeasons.toList(), formState.selectedSubcategory,
                formState.selectedFit, formState.selectedMaterial)
        },
    )
}

private class AddItemFormState(existingItem: ClothingItem?) {
    /** Decoded off the main thread, so it arrives after construction and must be state. */
    var existingPhotoBitmap by mutableStateOf<ImageBitmap?>(null)
    var photoBytes by mutableStateOf<ByteArray?>(null)
    var originalPhotoBytes by mutableStateOf<ByteArray?>(null)
    var photoBitmap by mutableStateOf<ImageBitmap?>(null)
    var bgRemoved by mutableStateOf(false)
    var isProcessingBg by mutableStateOf(false)
    var name by mutableStateOf(existingItem?.name ?: "")
    var selectedCategory by mutableStateOf(existingItem?.category)
    var selectedColors by mutableStateOf(existingItem?.colors?.toSet() ?: emptySet())
    var selectedSeasons by mutableStateOf(existingItem?.seasons?.toSet() ?: emptySet())
    var selectedSubcategory by mutableStateOf(existingItem?.subcategory)
    var selectedFit by mutableStateOf(existingItem?.fit)
    var selectedMaterial by mutableStateOf(existingItem?.material)
    var showSourceChooser by mutableStateOf(false)
    var showCropEditor by mutableStateOf(false)
    var showAiLockedSheet by mutableStateOf(false)
    val hasPhoto: Boolean get() = photoBytes != null || existingPhotoBitmap != null
}

@Composable
private fun rememberAddItemFormState(
    existingItem: ClothingItem?,
    prefillItem: ClothingItem?,
): AddItemFormState {
    // Only a stored item has a photo on disk; a prefill's photoPath is empty by construction.
    val existingPhotoBitmap = rememberDecodedImage(existingItem?.photoPath)
    val formState = remember { AddItemFormState(existingItem ?: prefillItem) }
    formState.existingPhotoBitmap = existingPhotoBitmap
    return formState
}

@Composable
private fun PhotoSourceChooser(
    show: Boolean,
    onDismiss: () -> Unit,
    onPhoto: (ByteArray, ImageBitmap) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            scope.launch {
                val bytes = readImageBytes(context, it) ?: return@launch
                val bitmap = decodePreviewImage(bytes) ?: return@launch
                onPhoto(bytes, bitmap)
            }
        }
    }

    val takePicture = rememberCameraCapture { bytes ->
        scope.launch {
            val bitmap = decodePreviewImage(bytes) ?: return@launch
            onPhoto(bytes, bitmap)
        }
    }

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
private fun AddItemFormContent(
    photoBitmap: ImageBitmap?,
    canCrop: Boolean,
    onCropClick: () -> Unit,
    canRemoveBackground: Boolean,
    bgRemoved: Boolean,
    isProcessingBg: Boolean,
    onRemoveBackgroundChange: (Boolean) -> Unit,
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
            .exposeTestTagsAsResourceId()
            .testTag("add_item_sheet")
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(if (isEditing) R.string.add_item_title_edit else R.string.add_item_title),
            color = WornColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        PhotoUploadZone(
            bitmap = photoBitmap,
            onClick = onPhotoClick,
            isProcessing = isProcessingBg,
            modifier = Modifier.testTag("add_item_photo_zone"),
        )
        if (canCrop) {
            CropPhotoButton(
                onClick = onCropClick,
                enabled = !isProcessingBg,
                modifier = Modifier.testTag("add_item_crop_button"),
            )
        }
        if (canRemoveBackground) {
            RemoveBackgroundToggle(
                checked = bgRemoved,
                enabled = !isProcessingBg,
                onCheckedChange = onRemoveBackgroundChange,
                modifier = Modifier.testTag("add_item_remove_bg_toggle"),
            )
        }
        if (!isEditing) {
            AiBadge(onClick = onAiBadgeClick, modifier = Modifier.testTag("add_item_ai_badge"))
        }
        ItemNameField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.testTag("add_item_name_field"),
        )
        CategoryDropdown(
            selected = selectedCategory,
            onSelected = onCategorySelected,
            modifier = Modifier.testTag("add_item_category_dropdown"),
        )
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
            label = if (isEditing) stringResource(R.string.common_save_changes) else null,
            modifier = Modifier.testTag("add_item_save_button"),
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
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.exposeTestTagsAsResourceId().testTag("photo_source_dialog"),
            ) {
                TextButton(
                    onClick = onCamera,
                    modifier = Modifier.fillMaxWidth().testTag("photo_source_camera"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.add_item_take_photo), fontSize = 16.sp)
                    }
                }
                TextButton(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth().testTag("photo_source_gallery"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.add_item_choose_gallery), fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

private inline fun <T> toggleInSet(item: T, current: Set<T>, update: (Set<T>) -> Unit) {
    update(if (item in current) current - item else current + item)
}

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
