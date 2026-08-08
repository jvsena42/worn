package com.github.worn.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.worn.R
import com.github.worn.ui.exposeTestTagsAsResourceId
import com.github.worn.ui.theme.PhonePreview
import com.github.worn.ui.theme.TabletPreview
import com.github.worn.ui.theme.WornTheme
import com.github.worn.ui.theme.wornExtras
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
                    // Crops the original bytes, not the downsampled preview, so the saved photo
                    // keeps its capture resolution.
                    val cropped = withContext(Dispatchers.Default) {
                        cropToJpeg(imageBytes, selection, bounds)
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
    val state = remember { CropSelectionState() }

    Column(
        modifier = Modifier
            .exposeTestTagsAsResourceId()
            .testTag("crop_editor")
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        CropEditorTopBar(
            canApply = state.isReady && !isProcessing,
            onCancel = onCancel,
            onApply = {
                val selection = state.selection ?: return@CropEditorTopBar
                val bounds = state.bounds ?: return@CropEditorTopBar
                onApply(selection, bounds)
            },
        )
        CropCanvas(
            bitmap = bitmap,
            state = state,
            isProcessing = isProcessing,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        CropEditorBottomBar(
            enabled = state.isReady && !isProcessing,
            onReset = state::reset,
        )
    }
}

@Composable
private fun CropCanvas(
    bitmap: ImageBitmap,
    state: CropSelectionState,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { HANDLE_TOUCH_RADIUS.toPx() }
    val minEdgePx = with(density) { MIN_CROP_EDGE.toPx() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag("crop_editor_canvas")
            // Pointer offsets are in px, so the whole geometry stays in px.
            .onSizeChanged { size ->
                state.fitTo(bitmap.width, bitmap.height, size.width.toFloat(), size.height.toFloat())
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> state.startDrag(offset, touchRadiusPx) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.drag(dragAmount, minEdgePx)
                    },
                    onDragEnd = state::endDrag,
                    onDragCancel = state::endDrag,
                )
            },
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        state.selection?.let { current ->
            Canvas(modifier = Modifier.fillMaxSize()) { drawCropOverlay(current) }
        }
        if (isProcessing) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
    }
}

/**
 * The live crop selection, in view-space px.
 *
 * Holding it here rather than in local composable state keeps the drag callbacks reading the
 * current bounds instead of a value captured when the gesture detector was launched.
 */
private class CropSelectionState {
    var bounds by mutableStateOf<CropViewRect?>(null)
        private set
    var selection by mutableStateOf<CropViewRect?>(null)
        private set

    private var activeCorner: CropCorner? = null

    val isReady: Boolean get() = bounds != null && selection != null

    /**
     * Resetting on resize/rotation keeps the selection inside the newly fitted image.
     *
     * Guarded on the bounds actually changing: the callback this runs from is re-invoked on
     * remeasure, and every drag recomposes the canvas, so an unconditional reset would snap the
     * selection back to the full image on the very next frame.
     */
    fun fitTo(imageWidth: Int, imageHeight: Int, viewWidth: Float, viewHeight: Float) {
        val fitted = CropGeometry.fitBounds(imageWidth, imageHeight, viewWidth, viewHeight)
        if (fitted == bounds) return
        bounds = fitted
        selection = fitted
    }

    fun startDrag(offset: Offset, touchRadius: Float) {
        activeCorner = selection?.let { cornerAt(offset, it, touchRadius) }
    }

    fun drag(dragAmount: Offset, minEdge: Float) {
        val base = selection ?: return
        val currentBounds = bounds ?: return
        // Compose reports incremental drags, so the base is the live selection.
        selection = activeCorner?.let { corner ->
            CropGeometry.resizeSelection(base, corner, dragAmount.x, dragAmount.y, currentBounds, minEdge)
        } ?: CropGeometry.moveSelection(base, dragAmount.x, dragAmount.y, currentBounds)
    }

    fun endDrag() {
        activeCorner = null
    }

    fun reset() {
        selection = bounds
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
            Text(
                stringResource(R.string.common_cancel),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Text(
            text = stringResource(R.string.crop_title),
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
        )
        TextButton(
            onClick = onApply,
            enabled = canApply,
            modifier = Modifier.testTag("crop_editor_apply"),
        ) {
            Text(
                text = stringResource(R.string.crop_apply),
                color = if (canApply) MaterialTheme.colorScheme.primary else MaterialTheme.wornExtras.textMuted,
                style = MaterialTheme.typography.titleSmall,
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
            Text(stringResource(R.string.crop_reset), color = Color.White, style = MaterialTheme.typography.bodyMedium)
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

// Preview-only stand-in for a real photo; the exact fill is irrelevant, so it stays a literal
// rather than a theme lookup that would force this helper to become @Composable.
private fun previewBitmap(): ImageBitmap =
    Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888)
        .apply { eraseColor(android.graphics.Color.rgb(0x7A, 0x94, 0x68)) }
        .asImageBitmap()

private const val PREVIEW_WIDTH = 600
private const val PREVIEW_HEIGHT = 800

@PhonePreview
@Composable
private fun CropEditorContentPhonePreview() {
    WornTheme { CropEditorContent(bitmap = previewBitmap()) }
}

@TabletPreview
@Composable
private fun CropEditorContentTabletPreview() {
    WornTheme { CropEditorContent(bitmap = previewBitmap()) }
}

