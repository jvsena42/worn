@file:OptIn(ExperimentalForeignApi::class)

package com.github.worn.data.source.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

private const val PERCENT = 100.0

actual class ImageDownscaler(private val dispatcher: CoroutineContext) {

    actual suspend fun downscale(bytes: ByteArray, maxEdge: Int): ByteArray =
        withContext(dispatcher) {
            // `imageWithData` rather than the `UIImage(data =)` constructor: cinterop maps the
            // constructor as non-null even though the underlying initialiser is failable, so an
            // undecodable payload would hand back a reference that crashes on first use.
            val image = UIImage.imageWithData(bytes.toNSData())
            val pixels = image?.pixelSize()
            when {
                image == null || pixels == null -> bytes
                max(pixels.width, pixels.height) <= maxEdge && bytes.isJpeg() -> bytes
                else -> image.redrawnAsJpeg(pixels, maxEdge) ?: bytes
            }
        }
}

/**
 * The image's dimensions in pixels rather than points, and already rotated to match how it will be
 * drawn — `UIImage.size` reports the oriented size, so a camera photo tagged `.right` reads as
 * portrait here even though its underlying `CGImage` buffer is landscape.
 */
private fun UIImage.pixelSize(): ScaledSize? = size.useContents {
    val pixelWidth = (width * this@pixelSize.scale).roundToInt()
    val pixelHeight = (height * this@pixelSize.scale).roundToInt()
    if (pixelWidth > 0 && pixelHeight > 0) ScaledSize(pixelWidth, pixelHeight) else null
}

/**
 * Redraws the image at (at most) [maxEdge] on its long edge and encodes it as JPEG.
 *
 * Drawing through `UIImage.draw(in:)` bakes in the EXIF orientation, so the model never receives a
 * sideways photo. `scale = 1` keeps the rendered bitmap at the size we asked for instead of
 * multiplying it by the device's screen scale, and `opaque = true` drops the alpha channel a JPEG
 * cannot carry anyway.
 */
private fun UIImage.redrawnAsJpeg(pixels: ScaledSize, maxEdge: Int): ByteArray? {
    val target = ImageScaling.fitLongEdge(pixels.width, pixels.height, maxEdge) ?: pixels
    val width = target.width.toDouble()
    val height = target.height.toDouble()

    val format = UIGraphicsImageRendererFormat.defaultFormat().apply {
        setScale(1.0)
        setOpaque(true)
    }
    val redrawn = UIGraphicsImageRenderer(size = CGSizeMake(width, height), format = format)
        .imageWithActions { drawInRect(CGRectMake(0.0, 0.0, width, height)) }

    return UIImageJPEGRepresentation(redrawn, AiImageLimits.JPEG_QUALITY / PERCENT)?.toByteArray()
}
