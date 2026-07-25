package com.github.worn.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import com.github.worn.R
import com.github.worn.ui.exposeTestTagsAsResourceId
import com.github.worn.ui.theme.WornColors
import com.github.worn.ui.theme.WornTheme
import com.github.worn.ui.util.cropToJpeg
import com.github.worn.ui.util.decodeForEditing
import com.github.worn.util.image.CropCorner
import com.github.worn.util.image.CropGeometry
import com.github.worn.util.image.CropViewRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Free-form crop editor for a photo already picked by one of the add-photo flows.
 *
 * Presented as a full-screen [Dialog] rather than a nested `ModalBottomSheet`: the add-item flow
 * opens this from inside an open sheet, and a sheet's drag-to-dismiss gesture would fight the crop
 * drags. A `Dialog` is its own window, so it has independent gestures and back handling — and it
 * mirrors the `.fullScreenCover` the iOS editor uses.
 *
 * [onCropped] receives freshly encoded JPEG bytes; the caller owns what to do with them.
 */
@Composable
fun CropEditorDialog(
    imageBytes: ByteArray,
    onCancel: () -> Unit,
    onCropped: (ByteArray) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    // Decoded once per photo so dragging never re-decodes.
    val bitmap = remember(imageBytes) { decodeForEditing(imageBytes) }

    if (bitmap == null) {
        LaunchedEffect(imageBytes) {
            Toast.makeText(context, context.getString(R.string.crop_failed), Toast.LENGTH_SHORT).show()
            onCancel()
        }
        return
    }

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CropEditorContent(
            bitmap = imageBitmap,
            isProcessing = isProcessing,
            onCancel = onCancel,
            onApply = { selection, bounds ->
                isProcessing = true
                scope.launch {
                    val cropped = withContext(Dispatchers.Default) {
                        val rect = CropGeometry.toSourceRect(selection, bounds, bitmap.width, bitmap.height)
                        cropToJpeg(bitmap, rect)
                    }
                    isProcessing = false
                    if (cropped != null) {
                        onCropped(cropped)
                    } else {
                        Toast.makeText(context, context.getString(R.string.crop_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }
}

@Composable
internal fun CropEditorContent(
    bitmap: ImageBitmap,
    isProcessing: Boolean = false,
    onCancel: () -> Unit = {},
    onApply: (selection: CropViewRect, bounds: CropViewRect) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { HANDLE_TOUCH_RADIUS.toPx() }
    val minEdgePx = with(density) { MIN_CROP_EDGE.toPx() }

    var bounds by remember { mutableStateOf<CropViewRect?>(null) }
    var selection by remember { mutableStateOf<CropViewRect?>(null) }
    var activeCorner by remember { mutableStateOf<CropCorner?>(null) }

    Column(
        modifier = Modifier
            .exposeTestTagsAsResourceId()
            .testTag("crop_editor")
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        CropEditorTopBar(
            canApply = selection != null && bounds != null && !isProcessing,
            onCancel = onCancel,
            onApply = {
                val currentSelection = selection ?: return@CropEditorTopBar
                val currentBounds = bounds ?: return@CropEditorTopBar
                onApply(currentSelection, currentBounds)
            },
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .testTag("crop_editor_canvas")
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { size ->
                    // Pointer offsets are in px, so the whole geometry stays in px.
                    val fitted = CropGeometry.fitBounds(
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height,
                        viewWidth = size.width.toFloat(),
                        viewHeight = size.height.toFloat(),
                    )
                    // Resetting on resize/rotation keeps the selection inside the newly fitted image.
                    bounds = fitted
                    selection = fitted
                }
                .pointerInput(bounds) {
                    val currentBounds = bounds ?: return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            activeCorner = selection?.let { cornerAt(offset, it, touchRadiusPx) }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val base = selection ?: return@detectDragGestures
                            // Compose reports incremental drags, so the base is the live selection.
                            selection = activeCorner?.let { corner ->
                                CropGeometry.resizeSelection(
                                    base, corner, dragAmount.x, dragAmount.y, currentBounds, minEdgePx,
                                )
                            } ?: CropGeometry.moveSelection(base, dragAmount.x, dragAmount.y, currentBounds)
                        },
                        onDragEnd = { activeCorner = null },
                        onDragCancel = { activeCorner = null },
                    )
                },
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            selection?.let { current ->
                Canvas(modifier = Modifier.fillMaxSize()) { drawCropOverlay(current) }
            }
            if (isProcessing) {
                CircularProgressIndicator(color = WornColors.AccentGreen, modifier = Modifier.size(36.dp))
            }
        }
        CropEditorBottomBar(
            enabled = bounds != null && !isProcessing,
            onReset = { selection = bounds },
        )
    }
}

@Composable
private fun CropEditorTopBar(canApply: Boolean, onCancel: () -> Unit, onApply: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.testTag("crop_editor_cancel")) {
            Text(stringResource(R.string.common_cancel), color = Color.White, fontSize = 16.sp)
        }
        Text(
            text = stringResource(R.string.crop_title),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(
            onClick = onApply,
            enabled = canApply,
            modifier = Modifier.testTag("crop_editor_apply"),
        ) {
            Text(
                text = stringResource(R.string.crop_apply),
                color = if (canApply) WornColors.AccentGreen else WornColors.TextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CropEditorBottomBar(enabled: Boolean, onReset: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        TextButton(onClick = onReset, enabled = enabled, modifier = Modifier.testTag("crop_editor_reset")) {
            Text(stringResource(R.string.crop_reset), color = Color.White, fontSize = 15.sp)
        }
    }
}

/** Dims everything outside [selection], then outlines it with thirds guides and corner handles. */
private fun DrawScope.drawCropOverlay(selection: CropViewRect) {
    clipRect(
        left = selection.left,
        top = selection.top,
        right = selection.right,
        bottom = selection.bottom,
        clipOp = ClipOp.Difference,
    ) {
        drawRect(color = Color.Black.copy(alpha = SCRIM_ALPHA))
    }

    val topLeft = Offset(selection.left, selection.top)
    val size = Size(selection.width, selection.height)
    drawRect(color = Color.White, topLeft = topLeft, size = size, style = Stroke(width = BORDER_WIDTH_PX))

    val thirdsColor = Color.White.copy(alpha = GUIDE_ALPHA)
    for (step in 1..2) {
        val x = selection.left + selection.width * step / 3f
        val y = selection.top + selection.height * step / 3f
        drawLine(thirdsColor, Offset(x, selection.top), Offset(x, selection.bottom), GUIDE_WIDTH_PX)
        drawLine(thirdsColor, Offset(selection.left, y), Offset(selection.right, y), GUIDE_WIDTH_PX)
    }

    drawCornerHandles(selection)
}

private fun DrawScope.drawCornerHandles(selection: CropViewRect) {
    val arm = minOf(HANDLE_ARM_PX, selection.width / 3f, selection.height / 3f)
    val corners = listOf(
        Triple(Offset(selection.left, selection.top), 1f, 1f),
        Triple(Offset(selection.right, selection.top), -1f, 1f),
        Triple(Offset(selection.left, selection.bottom), 1f, -1f),
        Triple(Offset(selection.right, selection.bottom), -1f, -1f),
    )
    corners.forEach { (corner, dirX, dirY) ->
        drawLine(Color.White, corner, corner.copy(x = corner.x + arm * dirX), HANDLE_WIDTH_PX)
        drawLine(Color.White, corner, corner.copy(y = corner.y + arm * dirY), HANDLE_WIDTH_PX)
    }
}

/** The corner of [selection] within [radius] of [offset], or null when the drag is a move. */
private fun cornerAt(offset: Offset, selection: CropViewRect, radius: Float): CropCorner? {
    val candidates = mapOf(
        CropCorner.TOP_LEFT to Offset(selection.left, selection.top),
        CropCorner.TOP_RIGHT to Offset(selection.right, selection.top),
        CropCorner.BOTTOM_LEFT to Offset(selection.left, selection.bottom),
        CropCorner.BOTTOM_RIGHT to Offset(selection.right, selection.bottom),
    )
    return candidates.entries
        .filter { (_, point) -> (point - offset).getDistance() <= radius }
        .minByOrNull { (_, point) -> (point - offset).getDistance() }
        ?.key
}

private val HANDLE_TOUCH_RADIUS = 28.dp
private val MIN_CROP_EDGE = 48.dp
private const val SCRIM_ALPHA = 0.55f
private const val GUIDE_ALPHA = 0.35f
private const val BORDER_WIDTH_PX = 3f
private const val GUIDE_WIDTH_PX = 1.5f
private const val HANDLE_WIDTH_PX = 8f
private const val HANDLE_ARM_PX = 56f

private fun previewBitmap(): ImageBitmap =
    Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888)
        .apply { eraseColor(WornColors.AccentGreen.toArgb()) }
        .asImageBitmap()

private const val PREVIEW_WIDTH = 600
private const val PREVIEW_HEIGHT = 800

@Preview(showSystemUi = true, device = "id:pixel_8")
@Composable
private fun CropEditorContentPhonePreview() {
    WornTheme { CropEditorContent(bitmap = previewBitmap()) }
}

@Preview(showSystemUi = true, device = "id:pixel_tablet")
@Composable
private fun CropEditorContentTabletPreview() {
    WornTheme { CropEditorContent(bitmap = previewBitmap()) }
}
