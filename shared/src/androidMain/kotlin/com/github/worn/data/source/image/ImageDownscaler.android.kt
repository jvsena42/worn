package com.github.worn.data.source.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

actual class ImageDownscaler(private val dispatcher: CoroutineContext) {

    actual suspend fun downscale(bytes: ByteArray, maxEdge: Int): ByteArray =
        withContext(dispatcher) {
            if (alreadyFits(bytes, maxEdge)) bytes else resize(bytes, maxEdge) ?: bytes
        }

    /**
     * Reads just the header to decide whether any work is needed.
     *
     * The long edge is the same either way round, so this holds without knowing the EXIF
     * orientation — which [BitmapFactory] does not report.
     */
    private fun alreadyFits(bytes: ByteArray, maxEdge: Int): Boolean {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
        }
        val longEdge = max(bounds.outWidth, bounds.outHeight)
        return longEdge in 1..maxEdge && bytes.isJpeg()
    }

    /**
     * Returns null when the image cannot be decoded, leaving the caller to send the original bytes
     * and let the API report what is wrong with them.
     *
     * [ImageDecoder] rather than [BitmapFactory] because it applies EXIF orientation while
     * decoding. `BitmapFactory` ignores it, so re-encoding through that path would drop the tag
     * and hand the model a sideways photo — the file on disk looks fine, only the request is
     * rotated, which is a miserable bug to track down.
     */
    private fun resize(bytes: ByteArray, maxEdge: Int): ByteArray? = runCatching {
        val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // A hardware bitmap cannot be re-scaled or read back on the CPU.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(
                ImageScaling.sampleSizeFor(info.size.width, info.size.height, maxEdge),
            )
        }
        // Measured against the decoded bitmap, so orientation has already been applied.
        val target = ImageScaling.fitLongEdge(decoded.width, decoded.height, maxEdge)
        val scaled = target
            ?.let { Bitmap.createScaledBitmap(decoded, it.width, it.height, true) }
            ?: decoded

        ByteArrayOutputStream().use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, AiImageLimits.JPEG_QUALITY, stream)
            stream.toByteArray()
        }
    }.getOrNull()
}
