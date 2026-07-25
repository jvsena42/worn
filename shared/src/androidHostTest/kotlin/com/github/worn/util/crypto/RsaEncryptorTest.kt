package com.github.worn.util.crypto

import java.security.KeyPairGenerator
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalEncodingApi::class)
class RsaEncryptorTest {

    @Test
    fun `encrypt output decrypts back to the original plaintext`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val publicKeyBase64 = Base64.encode(keyPair.public.encoded) // X.509 SubjectPublicKeyInfo
        val plaintext = "client_id=abc123&timestamp=1721890000000"

        val cipherBase64 = RsaEncryptor().encrypt(plaintext, publicKeyBase64)

        val decrypted = Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
            init(Cipher.DECRYPT_MODE, keyPair.private)
            doFinal(Base64.decode(cipherBase64)).decodeToString()
        }
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `PKCS1 padding produces a different ciphertext each call`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val publicKeyBase64 = Base64.encode(keyPair.public.encoded)
        val encryptor = RsaEncryptor()

        val first = encryptor.encrypt("same-input", publicKeyBase64)
        val second = encryptor.encrypt("same-input", publicKeyBase64)

        assert(first != second) { "PKCS#1 v1.5 padding is randomized, so ciphertexts must differ" }
    }
}
