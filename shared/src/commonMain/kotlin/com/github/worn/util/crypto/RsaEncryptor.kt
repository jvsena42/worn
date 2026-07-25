package com.github.worn.util.crypto

/**
 * Encrypts short plaintext with an RSA public key, for building the YouCam `id_token`.
 *
 * The public key is the user's YouCam `client_secret`: a Base64-encoded X.509 (SubjectPublicKeyInfo)
 * RSA key. Encryption uses PKCS#1 v1.5 padding and the result is Base64-encoded.
 *
 * Implemented per-platform because RSA lives in the platform crypto stack (JCA on Android, the
 * Security framework on iOS) rather than in `kotlin.crypto`.
 */
expect class RsaEncryptor() {
    fun encrypt(plaintext: String, publicKeyBase64: String): String
}
