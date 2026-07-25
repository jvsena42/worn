package com.github.worn.util.image

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A rectangle in *view* coordinates — pixels on Android, points on iOS.
 *
 * The origin is the top-left of the view that hosts the crop editor, so this is the space both
 * `onSizeChanged`/pointer offsets (Compose) and `GeometryReader`/`DragGesture` (SwiftUI) report in.
 */
data class CropViewRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** A rectangle in *source image pixel* coordinates, origin top-left. Always inside the image. */
data class CropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

/** The corner being dragged during a resize. */
enum class CropCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * Pure geometry behind the free-form crop editor, shared by Android and iOS.
 *
 * Only the *math* is shared: mapping a view-space selection onto source pixels is where crop bugs
 * live (letterbox offsets, rounding past the image edge), it has no platform types, and it can be
 * verified once in `commonTest` for every target. The gesture UI and the actual pixel work stay in
 * each platform's UI layer, where they can use `Bitmap`/`UIImage` directly instead of paying for an
 * extra decode to cross an `expect`/`actual` boundary.
 *
 * ### Delta-on-base contract
 *
 * [moveSelection] and [resizeSelection] apply a delta to an explicit `base` rect rather than
 * mutating a running selection, because the two platforms report drags differently:
 *
 * - Compose `detectDragGestures` gives an **incremental** `dragAmount` per frame, so Android passes
 *   `base = the current selection` and `delta = dragAmount`.
 * - SwiftUI `DragGesture.translation` is **cumulative** from the gesture's start, so iOS passes
 *   `base = the selection captured at drag start` and `delta = value.translation`.
 *
 * Passing the wrong base makes the selection accelerate away from the finger (iOS) or lag behind
 * it (Android), so keep this straight at each call site.
 */
object CropGeometry {

    /**
     * The rect the image occupies once fitted (letterboxed/pillarboxed) inside a view of
     * [viewWidth] x [viewHeight], preserving aspect ratio. This is also the maximum selection.
     */
    fun fitBounds(imageWidth: Int, imageHeight: Int, viewWidth: Float, viewHeight: Float): CropViewRect {
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0f || viewHeight <= 0f) {
            return CropViewRect(0f, 0f, viewWidth, viewHeight)
        }
        val scale = min(viewWidth / imageWidth, viewHeight / imageHeight)
        val drawnWidth = imageWidth * scale
        val drawnHeight = imageHeight * scale
        val left = (viewWidth - drawnWidth) / 2f
        val top = (viewHeight - drawnHeight) / 2f
        return CropViewRect(left, top, left + drawnWidth, top + drawnHeight)
    }

    /** Translates [base] by ([dx], [dy]), keeping its size and clamping it inside [bounds]. */
    fun moveSelection(base: CropViewRect, dx: Float, dy: Float, bounds: CropViewRect): CropViewRect {
        val maxLeft = bounds.right - base.width
        val maxTop = bounds.bottom - base.height
        val left = (base.left + dx).coerceIn(bounds.left, max(bounds.left, maxLeft))
        val top = (base.top + dy).coerceIn(bounds.top, max(bounds.top, maxTop))
        return CropViewRect(left, top, left + base.width, top + base.height)
    }

    /**
     * Drags [corner] of [base] by ([dx], [dy]). The opposite corner stays put, the result never
     * escapes [bounds], and neither edge shrinks below [minEdge].
     */
    fun resizeSelection(
        base: CropViewRect,
        corner: CropCorner,
        dx: Float,
        dy: Float,
        bounds: CropViewRect,
        minEdge: Float,
    ): CropViewRect {
        val edge = min(minEdge, min(bounds.width, bounds.height))
        return when (corner) {
            CropCorner.TOP_LEFT -> base.copy(
                left = (base.left + dx).coerceIn(bounds.left, base.right - edge),
                top = (base.top + dy).coerceIn(bounds.top, base.bottom - edge),
            )
            CropCorner.TOP_RIGHT -> base.copy(
                top = (base.top + dy).coerceIn(bounds.top, base.bottom - edge),
                right = (base.right + dx).coerceIn(base.left + edge, bounds.right),
            )
            CropCorner.BOTTOM_LEFT -> base.copy(
                left = (base.left + dx).coerceIn(bounds.left, base.right - edge),
                bottom = (base.bottom + dy).coerceIn(base.top + edge, bounds.bottom),
            )
            CropCorner.BOTTOM_RIGHT -> base.copy(
                right = (base.right + dx).coerceIn(base.left + edge, bounds.right),
                bottom = (base.bottom + dy).coerceIn(base.top + edge, bounds.bottom),
            )
        }
    }

    /**
     * Maps [selection] — expressed in the same view space as [bounds] — onto pixels of the
     * [imageWidth] x [imageHeight] source.
     *
     * The result is guaranteed to be non-empty and fully inside the image: `Bitmap.createBitmap`
     * throws and `CGImage.cropping` returns nil for a rect that escapes, and float rounding at the
     * edges makes that easy to hit.
     */
    fun toSourceRect(
        selection: CropViewRect,
        bounds: CropViewRect,
        imageWidth: Int,
        imageHeight: Int,
    ): CropRect {
        if (imageWidth <= 0 || imageHeight <= 0 || bounds.width <= 0f || bounds.height <= 0f) {
            return CropRect(0, 0, max(1, imageWidth), max(1, imageHeight))
        }
        val scaleX = imageWidth / bounds.width
        val scaleY = imageHeight / bounds.height
        val left = ((selection.left - bounds.left) * scaleX).roundToInt().coerceIn(0, imageWidth - 1)
        val top = ((selection.top - bounds.top) * scaleY).roundToInt().coerceIn(0, imageHeight - 1)
        val width = (selection.width * scaleX).roundToInt().coerceIn(1, imageWidth - left)
        val height = (selection.height * scaleY).roundToInt().coerceIn(1, imageHeight - top)
        return CropRect(left, top, width, height)
    }
}
