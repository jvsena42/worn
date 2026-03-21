package com.github.worn.util.secret

interface SecretStore {
    fun getApiKey(): String?
    fun saveApiKey(key: String)
    fun clearApiKey()
}
