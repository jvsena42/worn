package com.github.worn.fake

import com.github.worn.domain.model.GarmentCategory
import com.github.worn.domain.repository.TryOnRepository

class FakeTryOnRepository : TryOnRepository {
    var result: Result<ByteArray> = Result.success(byteArrayOf())
    var verifyResult: Result<Unit> = Result.success(Unit)
    val calls = mutableListOf<Pair<ByteArray, GarmentCategory>>()
    val verifiedCredentials = mutableListOf<Pair<String, String>>()

    override suspend fun generateTryOn(
        garmentBytes: ByteArray,
        category: GarmentCategory,
    ): Result<ByteArray> {
        calls.add(garmentBytes to category)
        return result
    }

    override suspend fun verifyCredentials(clientId: String, clientSecret: String): Result<Unit> {
        verifiedCredentials.add(clientId to clientSecret)
        return verifyResult
    }
}
