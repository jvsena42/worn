package com.github.worn.data.source.local

expect class PhotoFileStorage {
    suspend fun write(fileName: String, bytes: ByteArray): String
    suspend fun read(filePath: String): ByteArray
    suspend fun delete(filePath: String)
}
