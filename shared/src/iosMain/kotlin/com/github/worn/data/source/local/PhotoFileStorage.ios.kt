@file:OptIn(ExperimentalForeignApi::class)

package com.github.worn.data.source.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

actual class PhotoFileStorage {
    private val fileManager = NSFileManager.defaultManager

    private val photosDir: String
        get() {
            val documentsDir = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory, NSUserDomainMask, true,
            ).first() as String
            val dir = "$documentsDir/photos"
            fileManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
            return dir
        }

    actual suspend fun write(fileName: String, bytes: ByteArray): String {
        val filePath = "$photosDir/$fileName"
        val data = bytes.toNSData()
        data.writeToFile(filePath, atomically = true)
        return filePath
    }

    actual suspend fun read(filePath: String): ByteArray {
        val data = NSData.dataWithContentsOfFile(filePath)
            ?: error("File not found: $filePath")
        return data.toByteArray()
    }

    actual suspend fun delete(filePath: String) {
        fileManager.removeItemAtPath(filePath, error = null)
    }
}

private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, length)
        }
    }
    return bytes
}
