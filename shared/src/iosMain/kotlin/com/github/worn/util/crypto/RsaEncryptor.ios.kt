@file:OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)

package com.github.worn.util.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyCreateWithData
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecKeyAlgorithmRSAEncryptionPKCS1
import platform.posix.memcpy
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class RsaEncryptor {

    actual fun encrypt(plaintext: String, publicKeyBase64: String): String = memScoped {
        val pkcs1 = Base64.decode(publicKeyBase64).stripSpkiHeader()

        val keyData = pkcs1.toCFData()
        val plainData = plaintext.encodeToByteArray().toCFData()
        try {
            val attributes = CFDictionaryCreateMutable(null, 2, null, null)
            CFDictionaryAddValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeRSA)
            CFDictionaryAddValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)

            val errorPtr = alloc<CFErrorRefVar>()
            val secKey = SecKeyCreateWithData(keyData, attributes, errorPtr.ptr)
                ?: error("Could not import RSA public key")

            val cipher = try {
                SecKeyCreateEncryptedData(
                    secKey,
                    kSecKeyAlgorithmRSAEncryptionPKCS1,
                    plainData,
                    errorPtr.ptr,
                ) ?: error("RSA encryption failed")
            } finally {
                CFRelease(secKey)
            }

            val cipherBytes = (CFBridgingRelease(cipher) as NSData).toByteArray()
            Base64.encode(cipherBytes)
        } finally {
            CFRelease(keyData)
            CFRelease(plainData)
        }
    }
}

/**
 * iOS `SecKeyCreateWithData` expects a PKCS#1 `RSAPublicKey`, whereas YouCam supplies the X.509
 * `SubjectPublicKeyInfo` (SPKI) form that Java's `X509EncodedKeySpec` consumes. Walk the ASN.1 TLV
 * structure and return the inner `RSAPublicKey` (the BIT STRING contents, minus its unused-bits byte).
 */
private fun ByteArray.stripSpkiHeader(): ByteArray {
    var idx = 0

    fun readLength(): Int {
        val first = this[idx++].toInt() and 0xFF
        if (first < 0x80) return first
        var length = 0
        repeat(first and 0x7F) { length = (length shl 8) or (this[idx++].toInt() and 0xFF) }
        return length
    }

    require(this[idx++].toInt() and 0xFF == TAG_SEQUENCE) { "Expected SPKI SEQUENCE" }
    readLength()
    require(this[idx++].toInt() and 0xFF == TAG_SEQUENCE) { "Expected AlgorithmIdentifier SEQUENCE" }
    idx += readLength() // skip the whole AlgorithmIdentifier
    require(this[idx++].toInt() and 0xFF == TAG_BIT_STRING) { "Expected BIT STRING" }
    val bitStringLength = readLength()
    idx++ // drop the unused-bits count byte (always 0 for a key)
    return copyOfRange(idx, idx + bitStringLength - 1)
}

private fun ByteArray.toCFData(): CFDataRef = usePinned { pinned ->
    CFDataCreate(kCFAllocatorDefault, pinned.addressOf(0).reinterpret(), size.convert())
} ?: error("Could not allocate CFData")

private fun NSData.toByteArray(): ByteArray {
    val out = ByteArray(length.toInt())
    if (out.isNotEmpty()) {
        out.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
    return out
}

private const val TAG_SEQUENCE = 0x30
private const val TAG_BIT_STRING = 0x03
