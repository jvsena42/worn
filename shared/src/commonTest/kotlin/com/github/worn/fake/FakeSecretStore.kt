package com.github.worn.fake

import com.github.worn.util.secret.SecretStore

class FakeSecretStore(
    val secrets: MutableMap<String, String> = mutableMapOf(),
) : SecretStore {
    override fun getSecret(name: String): String? = secrets[name]
    override fun saveSecret(name: String, value: String) { secrets[name] = value }
    override fun clearSecret(name: String) { secrets.remove(name) }
}
