@file:OptIn(ExperimentalForeignApi::class)

package com.github.worn.util.secret

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecClassKey
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.Security.kSecClass

class IosSecretStore : SecretStore {

    override fun getApiKey(): String? = memScoped {
        val query = createQuery()
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)

        if (status == errSecSuccess) {
            val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
            NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String
        } else {
            null
        }
    }

    override fun saveApiKey(key: String) {
        val keyData = (key as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        // Try to update existing item first
        val query = createQuery()
        val update = CFDictionaryCreateMutable(null, 1, null, null)
        CFDictionaryAddValue(update, kSecValueData, CFBridgingRetain(keyData))

        val updateStatus = SecItemUpdate(query, update)
        if (updateStatus == errSecItemNotFound) {
            // Item doesn't exist, add it
            val addQuery = createQuery()
            CFDictionaryAddValue(addQuery, kSecValueData, CFBridgingRetain(keyData))
            SecItemAdd(addQuery, null)
        }
    }

    override fun clearApiKey() {
        val query = createQuery()
        SecItemDelete(query)
    }

    private fun createQuery() = CFDictionaryCreateMutable(null, QUERY_CAPACITY, null, null).also {
        CFDictionaryAddValue(it, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(it, kSecAttrService, CFBridgingRetain(SERVICE as NSString))
        CFDictionaryAddValue(it, kSecAttrAccount, CFBridgingRetain(ACCOUNT as NSString))
    }

    companion object {
        private const val SERVICE = "com.github.worn"
        private const val ACCOUNT = "claude_api_key"
        private const val QUERY_CAPACITY = 5L
    }
}
