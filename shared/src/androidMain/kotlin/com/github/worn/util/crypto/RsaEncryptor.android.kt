package com.github.worn.util.crypto

import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual class RsaEncryptor {
    actual fun encrypt(plaintext: String, publicKeyBase64: String): String {
        val keyBytes = Base64.decode(publicKeyBase64)
        val publicKey = KeyFactory.getInstance(ALGORITHM)
            .generatePublic(X509EncodedKeySpec(keyBytes))
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, publicKey)
        }
        val encrypted = cipher.doFinal(plaintext.encodeToByteArray())
        return Base64.encode(encrypted)
    }

    private companion object {
        const val ALGORITHM = "RSA"
        const val TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    }
}
