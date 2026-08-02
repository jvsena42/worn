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
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.security.KeyPairGenerator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class YouCamApiClientTest {

    private val resultJpeg = byteArrayOf(9, 8, 7, 6)

    private fun rsaPublicKeyBase64(): String {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        return Base64.encode(keyPair.public.encoded)
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    /**
     * Serves the full try-on sequence for exactly one feature, matching the *whole* path rather than
     * a `/file/` substring — a wrong API version or feature name has to 404 here, otherwise a routing
     * regression like the one behind issue #45 sails through the tests. Records every call and the
     * task-creation body for assertion.
     */
    private class TryOnEngine(feature: String) {
        val calls = mutableListOf<String>()
        var taskBody: String = ""
            private set

        private val filePath = "/s2s/v2.0/file/$feature"
        private val taskPath = "/s2s/v2.0/task/$feature"

        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            calls.add("${request.method.value} ${request.url.host}$path")
            when {
                path == "/s2s/v1.0/client/auth" ->
                    respond("""{"result":{"access_token":"tok"}}""", HttpStatusCode.OK, jsonHeadersOf())
                request.method == HttpMethod.Put && request.url.host == "upload.example" ->
                    respond("", HttpStatusCode.OK)
                path == filePath && request.method == HttpMethod.Post ->
                    respond(
                        """{"data":{"files":[{"file_id":"fid","requests":[""" +
                            """{"url":"https://upload.example/put","method":"PUT",""" +
                            """"headers":{"Content-Type":"image/jpeg"}}]}]}}""",
                        HttpStatusCode.OK,
                        jsonHeadersOf(),
                    )
                path == taskPath && request.method == HttpMethod.Post -> {
                    taskBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                    respond("""{"data":{"task_id":"task-1"}}""", HttpStatusCode.OK, jsonHeadersOf())
                }
                path == "$taskPath/task-1" && request.method == HttpMethod.Get ->
                    respond(
                        """{"data":{"task_status":"success","results":{"url":"https://cdn.example/r.jpg"}}}""",
                        HttpStatusCode.OK,
                        jsonHeadersOf(),
                    )
                request.url.host == "cdn.example" ->
                    respond(byteArrayOf(9, 8, 7, 6), HttpStatusCode.OK)
                else -> respond("not found", HttpStatusCode.NotFound)
            }
        }

        private fun jsonHeadersOf() = headersOf(HttpHeaders.ContentType, "application/json")
    }

    @Test
    fun `tryOn runs auth, uploads, task and poll then returns the image`() = runTest {
        val mock = TryOnEngine(feature = "cloth-v3")

        val result = client(mock.engine).tryOn(byteArrayOf(1), byteArrayOf(2), GarmentCategory.TOP)

        assertContentEquals(resultJpeg, result)
        assertTrue(mock.calls.any { it.contains("/client/auth") })
        assertEquals(2, mock.calls.count { it.contains("/s2s/v2.0/file/cloth-v3") && it.startsWith("POST") })
        assertTrue(mock.calls.any { it == "POST yce-api-01.perfectcorp.com/s2s/v2.0/task/cloth-v3" })
        assertTrue(mock.calls.any { it == "GET yce-api-01.perfectcorp.com/s2s/v2.0/task/cloth-v3/task-1" })
    }

    @Test
    fun `tryOn sends garment_category and no shoes fields for clothes`() = runTest {
        val mock = TryOnEngine(feature = "cloth-v3")

        client(mock.engine).tryOn(byteArrayOf(1), byteArrayOf(2), GarmentCategory.TOP)

        assertTrue(mock.taskBody.contains(""""garment_category":"upper_body""""), mock.taskBody)
        assertFalse(mock.taskBody.contains("gender"), mock.taskBody)
        assertFalse(mock.taskBody.contains("style"), mock.taskBody)
    }

    @Test
    fun `tryOn routes shoes to the v2 shoes feature with gender and style`() = runTest {
        val mock = TryOnEngine(feature = "shoes")

        val result = client(mock.engine).tryOn(byteArrayOf(1), byteArrayOf(2), GarmentCategory.SHOES)

        assertContentEquals(resultJpeg, result)
        assertEquals(2, mock.calls.count { it.contains("/s2s/v2.0/file/shoes") && it.startsWith("POST") })
        assertTrue(mock.calls.any { it == "POST yce-api-01.perfectcorp.com/s2s/v2.0/task/shoes" })
        assertTrue(mock.taskBody.contains(""""gender":"male""""), mock.taskBody)
        assertTrue(mock.taskBody.contains(""""style":"random""""), mock.taskBody)
        assertFalse(mock.taskBody.contains("garment_category"), mock.taskBody)
    }

    @Test
    fun `tryOn gives a friendly message when a response has an unexpected shape`() = runTest {
        // The exact v1.0-style `result` wrapper that produced the crash in issue #45.
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/client/auth") ->
                    respond("""{"result":{"access_token":"tok"}}""", HttpStatusCode.OK, jsonHeaders())
                else -> respond("""{"result":{}}""", HttpStatusCode.OK, jsonHeaders())
            }
        }

        val error = runCatching { client(engine).tryOn(byteArrayOf(1), byteArrayOf(2), GarmentCategory.SHOES) }
            .exceptionOrNull()

        assertFalse(error?.message.orEmpty().contains("com.github.worn"), error?.message.orEmpty())
        assertTrue(error?.message?.contains("unexpected response", ignoreCase = true) == true)
    }

    @Test
    fun `verifyCredentials succeeds when auth returns a token`() = runTest {
        val engine = MockEngine { respond("""{"result":{"access_token":"tok"}}""", HttpStatusCode.OK, jsonHeaders()) }
        val client = YouCamApiClient(HttpClient(engine), FakeSecretStore(), RsaEncryptor())
        // Throws on failure; reaching the end means success.
        client.verifyCredentials("client-123", rsaPublicKeyBase64())
    }

    @Test
    fun `verifyCredentials gives a friendly error for an unparseable secret key`() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val client = YouCamApiClient(HttpClient(engine), FakeSecretStore(), RsaEncryptor())
        val error = runCatching { client.verifyCredentials("client-123", "not-a-valid-key!!!") }
            .exceptionOrNull()
        assertTrue(error?.message?.contains("Secret Key", ignoreCase = true) == true)
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
