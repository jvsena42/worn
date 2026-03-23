package com.github.worn.fake

import com.github.worn.util.secret.SecretStore

class FakeSecretStore(
    var storedKey: String? = null,
) : SecretStore {
    override fun getApiKey(): String? = storedKey
    override fun saveApiKey(key: String) { storedKey = key }
    override fun clearApiKey() { storedKey = null }
}
