@file:OptIn(ExperimentalForeignApi::class)

package com.github.worn.data.source.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

/**
 * Bridges between Kotlin's [ByteArray] and Foundation's [NSData], which every UIKit/Vision imaging
 * entry point speaks. Shared by the imaging `actual`s in this package so the pinning dance is
 * written once.
 */
internal fun ByteArray.toNSData(): NSData =
    // `addressOf(0)` throws on an empty array, so the empty case cannot go through pinning.
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }

internal fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, length)
        }
    }
    return bytes
}
