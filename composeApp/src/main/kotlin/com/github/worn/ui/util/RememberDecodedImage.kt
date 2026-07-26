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
 * Decodes [bytes] into an [ImageBitmap] off the main thread, returning null until it is ready.
 *
 * Decoding inside `remember { }` runs during the composition pass, so a camera-resolution JPEG
 * stalls the frame that opens the sheet. [produceState] moves the work to [Dispatchers.Default]
 * and re-composes once with the result. Decoding goes through [decodeForEditing] so the bitmap is
 * downsampled rather than allocated at full resolution.
 */
@Composable
fun rememberDecodedImage(bytes: ByteArray?): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, bytes) {
        value = bytes?.let {
            withContext(Dispatchers.Default) { decodeForEditing(it)?.asImageBitmap() }
        }
    }
    return image
}

/** [rememberDecodedImage] for a stored photo path; the file read also happens off the main thread. */
@Composable
fun rememberDecodedImage(photoPath: String?): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, photoPath) {
        value = photoPath?.takeIf { it.isNotEmpty() }?.let { path ->
            withContext(Dispatchers.Default) {
                runCatching { File(path).readBytes() }
                    .getOrNull()
                    ?.let { decodeForEditing(it)?.asImageBitmap() }
            }
        }
    }
    return image
}
