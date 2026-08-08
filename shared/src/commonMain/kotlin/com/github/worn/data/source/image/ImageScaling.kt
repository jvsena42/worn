package com.github.worn.data.source.image

import kotlin.math.max
import kotlin.math.roundToInt

/** A target size in pixels, always at least 1x1. */
internal data class ScaledSize(val width: Int, val height: Int)

/**
 * The sizing decisions behind [ImageDownscaler], kept in common code and out of the `actual`s.
 *
 * Only the *math* is shared, for the same reason [com.github.worn.util.image.CropGeometry] is:
 * it has no platform types, it is where the off-by-one and aspect-ratio bugs live, and sharing it
 * is what stops Android and iOS from quietly producing different sizes for the same photo.
 */
internal object ImageScaling {

    /**
     * The size an [width] x [height] image should be drawn at so its long edge is [maxEdge],
     * or null when it already fits — callers use null to skip the resize entirely.
     */
    fun fitLongEdge(width: Int, height: Int, maxEdge: Int): ScaledSize? {
        val longEdge = max(width, height)
        if (width <= 0 || height <= 0 || longEdge <= maxEdge) return null
        val ratio = maxEdge.toDouble() / longEdge
        return ScaledSize(
            width = max(1, (width * ratio).roundToInt()),
            height = max(1, (height * ratio).roundToInt()),
        )
    }

    /**
     * The largest power-of-two subsample that still leaves the long edge at or above [maxEdge].
     *
     * Subsampling happens at decode time, so a 12MP photo never allocates its full bitmap. It only
     * gets within 2x of the target — [fitLongEdge] finishes the job on the much smaller result.
     */
    fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var longEdge = max(width, height)
        while (maxEdge > 0 && longEdge / 2 >= maxEdge) {
            longEdge /= 2
            sample *= 2
        }
        return sample
    }
}

/**
 * True when [this] starts with the JPEG SOI marker.
 *
 * Used to skip a pointless decode/re-encode round trip for an image that both fits already *and*
 * is already the format the API is told to expect.
 */
internal fun ByteArray.isJpeg(): Boolean =
    size >= JPEG_MAGIC.size && JPEG_MAGIC.indices.all { this[it] == JPEG_MAGIC[it] }

private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
