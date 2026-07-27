package com.github.worn.remote

import com.github.worn.data.source.remote.ClaudeApiClient
import com.github.worn.fake.FakeSecretStore
import com.github.worn.util.secret.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClaudeApiClientTest {

    private fun secretStore(): SecretStore =
        FakeSecretStore(mutableMapOf(SecretStore.CLAUDE_KEY to "sk-ant-test"))

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    private fun analysisResponse() = """
        {"content":[{"type":"text","text":"{\"description\":\"a tee\",
        \"suggested_category\":\"TOP\",\"colors\":[\"black\"],\"seasons\":[\"SUMMER\"],
        \"tags\":[\"casual\"]}"}]}
    """.trimIndent().replace("\n", "")

    /**
     * The image source's `type` and `media_type` are Kotlin default values. `Json` omits defaults
     * unless `encodeDefaults` is set, which would send a source block the API rejects.
     */
    @Test
    fun `analyzeImage sends a complete image source block`() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            body = (request.body as io.ktor.http.content.TextContent).text
            respond(ByteReadChannel(analysisResponse()), HttpStatusCode.OK, jsonHeaders())
        }

        ClaudeApiClient(HttpClient(engine), secretStore())
            .analyzeImage(byteArrayOf(1, 2, 3))

        assertTrue(body.contains("\"media_type\":\"image/jpeg\""), "missing media_type: $body")
        assertTrue(body.contains("\"type\":\"base64\""), "missing source type: $body")
    }

    @Test
    fun `requests target a supported model with thinking disabled`() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            body = (request.body as io.ktor.http.content.TextContent).text
            respond(ByteReadChannel(analysisResponse()), HttpStatusCode.OK, jsonHeaders())
        }

        ClaudeApiClient(HttpClient(engine), secretStore())
            .analyzeImage(byteArrayOf(1, 2, 3))

        assertTrue(body.contains("\"model\":\"claude-sonnet-5\""), "unexpected model: $body")
        assertTrue(body.contains("\"thinking\":{\"type\":\"disabled\"}"), "no thinking cfg: $body")
    }

    /** Null-valued blocks must be omitted, not serialised as explicit nulls. */
    @Test
    fun `text content blocks omit the unused image source`() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            body = (request.body as io.ktor.http.content.TextContent).text
            respond(ByteReadChannel(analysisResponse()), HttpStatusCode.OK, jsonHeaders())
        }

        ClaudeApiClient(HttpClient(engine), secretStore())
            .analyzeImage(byteArrayOf(1, 2, 3))

        assertFalse(body.contains("\"source\":null"), "explicit null source: $body")
        assertFalse(body.contains("\"text\":null"), "explicit null text: $body")
    }
}
