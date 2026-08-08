package com.github.worn.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Decodes [bytes] into a preview-sized [ImageBitmap] off the main thread.
 *
 * Stored photos keep their capture resolution, so decoding one at full size would allocate ~48MB
 * for a 12MP source just to fill a preview slot; [decodeForEditing] downsamples instead.
 */
suspend fun decodePreviewImage(bytes: ByteArray): ImageBitmap? =
    withContext(Dispatchers.Default) { decodeForEditing(bytes)?.asImageBitmap() }

/**
 * [decodePreviewImage] as composable state, returning null until the decode finishes.
 *
 * Decoding inside `remember { }` would run during the composition pass and stall the frame.
 */
@Composable
fun rememberDecodedImage(bytes: ByteArray?): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, bytes) {
        value = bytes?.let { decodePreviewImage(it) }
    }
    return image
}

/** [rememberDecodedImage] for a stored photo path; the file read is off the main thread too. */
@Composable
fun rememberDecodedImage(photoPath: String?): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, photoPath) {
        value = photoPath?.takeIf { it.isNotEmpty() }?.let { path ->
            withContext(Dispatchers.IO) { runCatching { File(path).readBytes() }.getOrNull() }
                ?.let { decodePreviewImage(it) }
        }
    }
    return image
}
