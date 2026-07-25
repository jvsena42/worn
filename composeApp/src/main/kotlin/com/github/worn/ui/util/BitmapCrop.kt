package com.github.worn.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.worn.util.image.CropRect
import java.io.ByteArrayOutputStream

private const val JPEG_QUALITY = 90
private const val DEFAULT_MAX_EDGE = 2048

/**
 * Decodes [bytes] for on-screen editing, downsampling so the long edge stays near [maxEdge].
 *
 * The crop editor holds the decoded bitmap for the whole gesture, so decoding a 12 MP photo at full
 * resolution would keep ~48 MB alive while the user drags. `inSampleSize` is applied at decode time
 * by `BitmapFactory`, so the full-size bitmap is never allocated.
 *
 * The caller must compute its crop rect against the *returned* bitmap's dimensions, not the
 * original file's.
 */
fun decodeForEditing(bytes: ByteArray, maxEdge: Int = DEFAULT_MAX_EDGE): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdge)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

/** The largest power-of-two subsample that still keeps the long edge at or above [maxEdge]. */
private fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    var longEdge = maxOf(width, height)
    while (longEdge / 2 >= maxEdge) {
        longEdge /= 2
        sample *= 2
    }
    return sample
}

/**
 * Crops [source] to [rect] and re-encodes as JPEG, matching the quality used everywhere else in the
 * photo pipeline. Returns null if the rect cannot be applied.
 */
fun cropToJpeg(source: Bitmap, rect: CropRect, quality: Int = JPEG_QUALITY): ByteArray? =
    runCatching {
        val cropped = Bitmap.createBitmap(source, rect.left, rect.top, rect.width, rect.height)
        ByteArrayOutputStream().use { stream ->
            cropped.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
    }.getOrNull()
