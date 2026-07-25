package com.github.worn.util.secret

/**
 * Platform-backed secure storage for user-provided secrets (BYOK), keyed by name so multiple
 * providers can coexist (e.g. the Claude API key and the YouCam credential pair).
 *
 * Implementations use the platform secret store: Android Keystore-encrypted file on Android,
 * Keychain on iOS. The Claude convenience accessors delegate to the named API to keep existing
 * call sites unchanged.
 */
interface SecretStore {
    fun getSecret(name: String): String?
    fun saveSecret(name: String, value: String)
    fun clearSecret(name: String)

    fun getApiKey(): String? = getSecret(CLAUDE_KEY)
    fun saveApiKey(key: String) = saveSecret(CLAUDE_KEY, key)
    fun clearApiKey() = clearSecret(CLAUDE_KEY)

    companion object {
        const val CLAUDE_KEY = "claude_api_key"
        const val YOUCAM_CLIENT_ID = "youcam_client_id"
        const val YOUCAM_CLIENT_SECRET = "youcam_client_secret"
    }
}
