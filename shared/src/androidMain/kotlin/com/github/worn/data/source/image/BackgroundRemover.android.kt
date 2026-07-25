package com.github.worn.data.source.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.CoroutineContext

actual class BackgroundRemover(private val dispatcher: CoroutineContext) {

    actual suspend fun removeBackground(bytes: ByteArray): ByteArray = withContext(dispatcher) {
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Could not decode image")

        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)

        val result = segmenter.process(InputImage.fromBitmap(source, 0)).await()
        val foreground = result.foregroundBitmap ?: error("No subject detected")

        // Composite the cut-out subject onto a solid neutral background and re-encode as JPEG.
        val output = Bitmap.createBitmap(foreground.width, foreground.height, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(NEUTRAL_BACKGROUND)
            drawBitmap(foreground, 0f, 0f, null)
        }

        ByteArrayOutputStream().use { stream ->
            output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            stream.toByteArray()
        }
    }

    private companion object {
        const val JPEG_QUALITY = 90
        const val NEUTRAL_BACKGROUND = Color.WHITE
    }
}
