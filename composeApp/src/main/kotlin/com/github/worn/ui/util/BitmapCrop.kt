package com.github.worn.ui.util

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import com.github.worn.util.image.CropGeometry
import com.github.worn.util.image.CropViewRect
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max

/** Stored photos keep their capture quality; only outbound API copies are reduced. */
private const val JPEG_QUALITY = 95
private const val DEFAULT_MAX_EDGE = 2048

/**
 * Decodes [bytes] for on-screen editing, downsampling so the long edge stays near [maxEdge].
 *
 * The crop editor holds the decoded bitmap for the whole gesture, so decoding a 12MP photo at full
 * resolution would keep ~48MB alive while the user drags. The subsampling is applied at decode
 * time, so the full-size bitmap is never allocated.
 *
 * This is a *preview*: never crop or save what it returns — [cropToJpeg] re-reads the original.
 */
fun decodeForEditing(bytes: ByteArray, maxEdge: Int = DEFAULT_MAX_EDGE): Bitmap? =
    decode(bytes) { info -> sampleSizeFor(info.size.width, info.size.height, maxEdge) }

/**
 * Crops the original [bytes] to the part of [bounds] covered by [selection] and encodes the result
 * as JPEG. Returns null if the photo cannot be decoded.
 *
 * Takes the encoded bytes rather than the preview bitmap so the saved photo keeps its capture
 * resolution — cropping the preview would bake the editor's ~2048px downsample into storage. It
 * also maps the selection against the full-size bitmap's own dimensions, which removes any chance
 * of the rect being measured against one image and applied to another.
 *
 * Decoding the whole photo costs a transient ~48MB for a 12MP source. That is deliberate:
 * `BitmapRegionDecoder` would read only the selected region, but it ignores EXIF orientation, so
 * the rect and the pixels would disagree for any camera photo that is not already upright.
 */
fun cropToJpeg(bytes: ByteArray, selection: CropViewRect, bounds: CropViewRect): ByteArray? {
    val source = decode(bytes) { 1 } ?: return null
    val rect = CropGeometry.toSourceRect(selection, bounds, source.width, source.height)
    return runCatching {
        val cropped = Bitmap.createBitmap(source, rect.left, rect.top, rect.width, rect.height)
        ByteArrayOutputStream().use { stream ->
            cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            stream.toByteArray()
        }
    }.getOrNull()
}

/**
 * Decodes [bytes] with the subsample [sampleSize] chooses from the header.
 *
 * `ImageDecoder` rather than `BitmapFactory` because it applies EXIF orientation. Without that, a
 * camera photo would appear sideways in the editor while the grid — which goes through Coil —
 * shows it upright, and the crop would land on the wrong axis.
 */
private fun decode(bytes: ByteArray, sampleSize: (ImageDecoder.ImageInfo) -> Int): Bitmap? =
    runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            // A hardware bitmap cannot be cropped or read back on the CPU.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(sampleSize(info))
        }
    }.getOrNull()

/** The largest power-of-two subsample that still keeps the long edge at or above [maxEdge]. */
private fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    var longEdge = max(width, height)
    while (longEdge / 2 >= maxEdge) {
        longEdge /= 2
        sample *= 2
    }
    return sample
}
