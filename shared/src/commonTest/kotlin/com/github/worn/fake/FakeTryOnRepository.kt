package com.github.worn.fake

import com.github.worn.domain.model.GarmentCategory
import com.github.worn.domain.repository.TryOnRepository

class FakeTryOnRepository : TryOnRepository {
    var result: Result<ByteArray> = Result.success(byteArrayOf())
    val calls = mutableListOf<Pair<ByteArray, GarmentCategory>>()

    override suspend fun generateTryOn(
        garmentBytes: ByteArray,
        category: GarmentCategory,
    ): Result<ByteArray> {
        calls.add(garmentBytes to category)
        return result
    }
}
