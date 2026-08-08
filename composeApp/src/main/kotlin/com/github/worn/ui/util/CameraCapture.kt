package com.github.worn.ui.util

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Launches the camera and hands back the captured photo at full resolution.
 *
 * `ActivityResultContracts.TakePicturePreview` — the obvious choice — returns the camera app's
 * *thumbnail* extra, typically a few hundred pixels. `TakePicture` instead writes the real photo
 * into a URI we supply, which is the only way to get the full-resolution frame. That costs a
 * scratch file and a [FileProvider], hence this helper rather than an inline launcher.
 *
 * The bytes are handed over as captured, EXIF and all — nothing is re-encoded, so no quality is
 * lost between the sensor and storage.
 */
@Composable
fun rememberCameraCapture(onPhoto: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // `TakePicture` reports only success/failure, so the target has to outlive the launch. Plain
    // state rather than `mutableStateOf`: nothing renders it, so a change should not recompose.
    val pending = remember { PendingCapture() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = pending.uri ?: return@rememberLauncherForActivityResult
        pending.uri = null
        scope.launch {
            val bytes = if (saved) readImageBytes(context, uri) else null
            withContext(Dispatchers.IO) { context.deleteCaptureFile() }
            bytes?.let(onPhoto)
        }
    }

    return remember(launcher, context) {
        {
            val uri = context.createCaptureUri()
            if (uri != null) {
                pending.uri = uri
                launcher.launch(uri)
            }
        }
    }
}

private class PendingCapture {
    var uri: Uri? = null
}

/**
 * A single reused scratch file, so a cancelled or crashed capture cannot leave the cache growing.
 * The name is fixed because only one capture can be in flight at a time.
 */
private const val CAPTURE_DIR = "camera"
private const val CAPTURE_FILE = "capture.jpg"

/** Null when the cache directory is unavailable, in which case the capture is simply not started. */
private fun Context.createCaptureUri(): Uri? = runCatching {
    val directory = File(cacheDir, CAPTURE_DIR).apply { mkdirs() }
    val file = File(directory, CAPTURE_FILE)
    FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}.getOrNull()

private fun Context.deleteCaptureFile() {
    runCatching { File(File(cacheDir, CAPTURE_DIR), CAPTURE_FILE).delete() }
}
