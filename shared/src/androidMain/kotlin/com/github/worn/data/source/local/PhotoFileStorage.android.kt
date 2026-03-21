package com.github.worn.data.source.local

import android.content.Context
import java.io.File

actual class PhotoFileStorage(private val context: Context) {
    private val photosDir: File
        get() = File(context.filesDir, "photos").also { it.mkdirs() }

    actual suspend fun write(fileName: String, bytes: ByteArray): String {
        val file = File(photosDir, fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual suspend fun read(filePath: String): ByteArray =
        File(filePath).readBytes()

    actual suspend fun delete(filePath: String) {
        File(filePath).delete()
    }
}
