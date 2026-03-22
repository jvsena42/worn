package com.github.worn.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.github.worn.ui.components.CategoryDropdown
import com.github.worn.ui.components.ColorSection
import com.github.worn.ui.components.ItemNameField
import com.github.worn.ui.components.PhotoUploadZone
import com.github.worn.ui.components.SaveButton
import com.github.worn.ui.components.SeasonSection
import com.github.worn.ui.theme.SheetPreview
import com.github.worn.ui.theme.WornColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    isSaving: Boolean,
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
        AddItemForm(isSaving = isSaving, onSave = onSave)
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
    onSave: (ByteArray, String, Category, List<String>, List<Season>) -> Unit =
        { _, _, _, _, _ -> },
) {
    val context = LocalContext.current
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var photoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedColors by remember { mutableStateOf(setOf<String>()) }
    var selectedSeasons by remember { mutableStateOf(setOf<Season>()) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes()
            if (bytes != null) {
                photoBytes = bytes
                photoBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?.asImageBitmap()
            }
        }
    }

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
        PhotoUploadZone(bitmap = photoBitmap, onClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        })
        AiBadge()
        ItemNameField(value = name, onValueChange = { name = it })
        CategoryDropdown(selected = selectedCategory, onSelected = { selectedCategory = it })
        ColorSection(
            selectedColors = selectedColors,
            onToggle = { toggleInSet(it, selectedColors) { selectedColors = it } },
        )
        SeasonSection(
            selectedSeasons = selectedSeasons,
            onToggle = { toggleInSet(it, selectedSeasons) { selectedSeasons = it } },
        )
        SaveButton(
            enabled = photoBytes != null && name.isNotBlank()
                && selectedCategory != null && !isSaving,
            isSaving = isSaving,
            onClick = {
                val bytes = photoBytes ?: return@SaveButton
                val cat = selectedCategory ?: return@SaveButton
                onSave(bytes, name, cat, selectedColors.toList(), selectedSeasons.toList())
            },
        )
    }
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
