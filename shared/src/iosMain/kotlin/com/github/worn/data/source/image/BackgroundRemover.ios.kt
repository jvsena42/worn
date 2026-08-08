@file:OptIn(ExperimentalForeignApi::class)

package com.github.worn.data.source.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.withContext
import platform.CoreImage.CIColor
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.Foundation.NSError
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.Vision.VNGenerateForegroundInstanceMaskRequest
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNInstanceMaskObservation
import kotlin.coroutines.CoroutineContext

actual class BackgroundRemover(private val dispatcher: CoroutineContext) {

    actual suspend fun removeBackground(bytes: ByteArray): ByteArray = withContext(dispatcher) {
        val uiImage = UIImage(data = bytes.toNSData()) ?: error("Could not decode image")
        val cgImage = uiImage.CGImage ?: error("Image has no CGImage")

        val request = VNGenerateForegroundInstanceMaskRequest()
        val handler = VNImageRequestHandler(cGImage = cgImage, options = emptyMap<Any?, Any?>())

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val ok = handler.performRequests(listOf(request), errorPtr.ptr)
            if (!ok) error("Vision request failed: ${errorPtr.value?.localizedDescription}")
        }

        val observation = request.results?.firstOrNull() as? VNInstanceMaskObservation
            ?: error("No subject detected")

        val maskedBuffer = memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            observation.generateMaskedImageOfInstances(
                instances = observation.allInstances,
                fromRequestHandler = handler,
                croppedToInstancesExtent = false,
                error = errorPtr.ptr,
            ) ?: error("Could not generate masked image: ${errorPtr.value?.localizedDescription}")
        }

        // The masked buffer keeps the subject and makes the background transparent.
        // Composite it over a solid neutral background so the saved JPEG has no alpha.
        val foreground = CIImage.imageWithCVPixelBuffer(maskedBuffer)
        val extent = foreground.extent
        val background = CIImage.imageWithColor(NEUTRAL_BACKGROUND).imageByCroppingToRect(extent)
        val composited = foreground.imageByCompositingOverImage(background)

        val context = CIContext()
        val outputCgImage = context.createCGImage(composited, fromRect = extent)
            ?: error("Could not render image")
        val jpeg = UIImageJPEGRepresentation(UIImage.imageWithCGImage(outputCgImage), JPEG_QUALITY)
            ?: error("Could not encode JPEG")
        jpeg.toByteArray()
    }

    private companion object {
        const val JPEG_QUALITY = 0.9
        val NEUTRAL_BACKGROUND = CIColor(red = 1.0, green = 1.0, blue = 1.0)
    }
}
