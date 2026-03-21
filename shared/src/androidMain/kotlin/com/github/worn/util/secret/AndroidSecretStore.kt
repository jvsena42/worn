package com.github.worn.util.secret

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidSecretStore(private val context: Context) : SecretStore {

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    override fun getApiKey(): String? {
        val contents = secretFile().takeIf { it.exists() }?.readBytes()
            ?.takeIf { it.size > GCM_IV_LENGTH }
            ?: return null
        val iv = contents.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = contents.copyOfRange(GCM_IV_LENGTH, contents.size)
        return decrypt(ciphertext, iv)
    }

    override fun saveApiKey(key: String) {
        val (ciphertext, iv) = encrypt(key.encodeToByteArray())
        secretFile().writeBytes(iv + ciphertext)
    }

    override fun clearApiKey() {
        secretFile().delete()
    }

    private fun secretFile(): File = File(context.filesDir, SECRET_FILE_NAME)

    private fun getOrCreateKey(): SecretKey {
        keyStore.getEntry(KEY_ALIAS, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(data)
        return encrypted to cipher.iv
    }

    private fun decrypt(data: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(data).decodeToString()
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "worn_api_key"
        private const val SECRET_FILE_NAME = "worn_secret.enc"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
    }
}
