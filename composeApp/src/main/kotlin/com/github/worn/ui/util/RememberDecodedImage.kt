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
 * Decoding inside `remember { }` would run during the composition pass and stall the frame.
 * [decodeForEditing] also downsamples rather than decoding at full resolution.
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

/** [rememberDecodedImage] for a stored photo path; the file read is off the main thread too. */
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
