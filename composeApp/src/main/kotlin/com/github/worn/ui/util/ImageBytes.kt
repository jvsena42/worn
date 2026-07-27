package com.github.worn.ui.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One photo handed to the app from the system share sheet. [bytes] is null when the URI could not
 * be read.
 *
 * Deliberately not a data class: identity equality makes every share a distinct value, so
 * `LaunchedEffect` re-fires even when the user shares the same photo twice in a row.
 */
class SharedPhoto(val bytes: ByteArray?)

/**
 * Reads an image URI — from the photo picker or a share intent — into bytes off the main thread.
 * Returns null when the URI is unreadable (revoked permission, deleted file, unsupported provider).
 *
 * Dispatchers.IO is hardcoded because this is Android UI glue with no DI graph; the
 * constructor-injected `CoroutineContext` convention applies to the data layer.
 */
suspend fun readImageBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching { context.contentResolver.openInputStream(uri)?.readBytes() }.getOrNull()
}
