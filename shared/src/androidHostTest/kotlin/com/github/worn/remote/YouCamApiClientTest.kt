package com.github.worn.remote

import com.github.worn.data.source.remote.YouCamApiClient
import com.github.worn.domain.model.GarmentCategory
import com.github.worn.fake.FakeSecretStore
import com.github.worn.util.crypto.RsaEncryptor
import com.github.worn.util.secret.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.security.KeyPairGenerator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class YouCamApiClientTest {

    private val resultJpeg = byteArrayOf(9, 8, 7, 6)

    private fun rsaPublicKeyBase64(): String {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        return Base64.encode(keyPair.public.encoded)
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `tryOn runs auth, uploads, task and poll then returns the image`() = runTest {
        val calls = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            calls.add("${request.method.value} ${request.url.host}$path")
            when {
                path.endsWith("/client/auth") ->
                    respond("""{"result":{"access_token":"tok"}}""", HttpStatusCode.OK, jsonHeaders())
                request.method == HttpMethod.Put ->
                    respond("", HttpStatusCode.OK)
                path.contains("/file/") ->
                    respond(
                        """{"result":{"files":[{"file_id":"fid","requests":[""" +
                            """{"url":"https://upload.example/put","method":"PUT",""" +
                            """"headers":{"Content-Type":"image/jpeg"}}]}]}}""",
                        HttpStatusCode.OK,
                        jsonHeaders(),
                    )
                path.contains("/task/") && request.method == HttpMethod.Post ->
                    respond("""{"result":{"task_id":"task-1"}}""", HttpStatusCode.OK, jsonHeaders())
                path.contains("/task/") && request.method == HttpMethod.Get ->
                    respond(
                        """{"result":{"status":"success","results":[{"url":"https://cdn.example/r.jpg"}]}}""",
                        HttpStatusCode.OK,
                        jsonHeaders(),
                    )
                request.url.host == "cdn.example" ->
                    respond(resultJpeg, HttpStatusCode.OK)
                else -> respond("not found", HttpStatusCode.NotFound)
            }
        }
        val client = client(engine)

        val result = client.tryOn(byteArrayOf(1), byteArrayOf(2), GarmentCategory.TOP)

        assertContentEquals(resultJpeg, result)
        assertTrue(calls.any { it.contains("/client/auth") })
        assertEquals(2, calls.count { it.contains("/file/") && it.startsWith("POST") })
        assertTrue(calls.any { it.startsWith("POST") && it.contains("/task/") })
        assertTrue(calls.any { it.startsWith("GET") && it.contains("/task/") })
    }

    @Test
    fun `tryOn surfaces a friendly message on auth failure`() = runTest {
        val engine = MockEngine { respond("nope", HttpStatusCode.Unauthorized) }
        val error = runCatching { client(engine).tryOn(byteArrayOf(1), byteArrayOf(2), GarmentCategory.TOP) }
            .exceptionOrNull()
        assertTrue(error?.message?.contains("credentials", ignoreCase = true) == true)
    }

    private fun client(engine: MockEngine): YouCamApiClient {
        val secretStore = FakeSecretStore().apply {
            saveSecret(SecretStore.YOUCAM_CLIENT_ID, "client-123")
            saveSecret(SecretStore.YOUCAM_CLIENT_SECRET, rsaPublicKeyBase64())
        }
        return YouCamApiClient(HttpClient(engine), secretStore, RsaEncryptor())
    }
}
