package com.github.worn.data.source.remote

import com.github.worn.domain.model.GarmentCategory
import com.github.worn.util.crypto.RsaEncryptor
import com.github.worn.util.secret.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

/**
 * Thin client for Perfect Corp's YouCam try-on APIs (BYOK). Holds no business logic: it authenticates
 * with the user's credentials, uploads the person + garment images via the presigned File API, creates
 * a try-on task, polls it, and returns the generated JPEG bytes. Errors are surfaced as user-facing
 * messages via [error]; the repository wraps calls in `runCatching`.
 */
@Suppress("TooManyFunctions")
class YouCamApiClient(
    private val httpClient: HttpClient,
    private val secretStore: SecretStore,
    private val rsaEncryptor: RsaEncryptor,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private var cachedToken: String? = null
    private var tokenExpiryMillis: Long = 0

    suspend fun tryOn(
        personBytes: ByteArray,
        garmentBytes: ByteArray,
        category: GarmentCategory,
    ): ByteArray {
        val feature = category.feature()
        log("tryOn: start category=$category feature=${feature.type}")
        val token = authenticate()
        val personFileId = uploadImage(token, feature, personBytes)
        val garmentFileId = uploadImage(token, feature, garmentBytes)
        val taskId = createTask(token, feature, personFileId, garmentFileId, category)
        val resultUrl = pollTask(token, feature, taskId)
        return downloadResult(resultUrl)
    }

    /** Performs an auth handshake to check the credentials work; throws a friendly message otherwise. */
    suspend fun verifyCredentials(clientId: String, clientSecret: String) {
        log("verifyCredentials: start")
        requestToken(clientId, clientSecret)
        log("verifyCredentials: ok")
    }

    private suspend fun authenticate(): String {
        val now = Clock.System.now().toEpochMilliseconds()
        cachedToken?.let { if (now < tokenExpiryMillis) return it }

        val clientId = secretStore.getSecret(SecretStore.YOUCAM_CLIENT_ID)
        val clientSecret = secretStore.getSecret(SecretStore.YOUCAM_CLIENT_SECRET)
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) {
            error("YouCam credentials not configured. Add them in Settings.")
        }

        val token = requestToken(clientId, clientSecret)
        cachedToken = token
        tokenExpiryMillis = now + TOKEN_TTL_MILLIS - TOKEN_REFRESH_MARGIN_MILLIS
        return token
    }

    private suspend fun requestToken(clientId: String, clientSecret: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        log("auth: id_token from client_id(len=${clientId.length}) secret(len=${clientSecret.length}) ts=$timestamp")
        val idToken = runCatching {
            rsaEncryptor.encrypt("client_id=$clientId&timestamp=$timestamp", clientSecret)
        }.getOrElse {
            log("auth: RSA encrypt failed: ${it.message}")
            error("Invalid Secret Key. Paste the long key without the -----BEGIN/END----- lines.")
        }
        val body = json.encodeToString(
            YouCamAuthRequest.serializer(),
            YouCamAuthRequest(clientId = clientId, idToken = idToken),
        )
        log("auth: POST /s2s/v1.0/client/auth (id_token len=${idToken.length})")
        val response = request("$BASE_URL/s2s/v1.0/client/auth") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        ensureSuccess(response, "auth")
        val token = json.decodeFromString<YouCamAuthResponse>(response.bodyAsText()).result.accessToken
        log("auth: ok, requesting upload slots")
        return token
    }

    private suspend fun uploadImage(token: String, feature: Feature, bytes: ByteArray): String {
        log("upload: creating slot (${bytes.size}b) at /s2s/${feature.version}/file/${feature.type}")
        val createResponse = request("$BASE_URL/s2s/${feature.version}/file/${feature.type}") {
            authorized(token)
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    YouCamFileRequest.serializer(),
                    YouCamFileRequest(
                        listOf(YouCamFileRequest.Spec(CONTENT_TYPE_JPEG, "image.jpg", bytes.size)),
                    ),
                ),
            )
        }
        ensureSuccess(createResponse, "upload/create")
        val createBody = createResponse.bodyAsText()
        log("upload/create ok: ${createBody.take(BODY_PREVIEW_LEN)}")
        val entry = json.decodeFromString<YouCamFileResponse>(createBody)
            .data.files.firstOrNull() ?: error("Upload could not be prepared. Please try again.")
        val upload = entry.requests.firstOrNull() ?: error("Upload could not be prepared. Please try again.")

        log("upload: PUT ${bytes.size}b to storage")
        val uploadResponse = request(upload.url, method = HttpMethod.PUT) {
            upload.headers.forEach { (name, value) -> header(name, value) }
            setBody(bytes)
        }
        ensureSuccess(uploadResponse, "upload/put")
        log("upload: ok file_id=${entry.fileId}")
        return entry.fileId
    }

    private suspend fun createTask(
        token: String,
        feature: Feature,
        personFileId: String,
        garmentFileId: String,
        category: GarmentCategory,
    ): String {
        val response = request("$BASE_URL/s2s/${feature.version}/task/${feature.type}") {
            authorized(token)
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    YouCamTaskRequest.serializer(),
                    YouCamTaskRequest(
                        srcFileId = personFileId,
                        refFileId = garmentFileId,
                        garmentCategory = category.garmentCategory(),
                    ),
                ),
            )
        }
        ensureSuccess(response, "task/create")
        val taskBody = response.bodyAsText()
        log("task/create ok: ${taskBody.take(BODY_PREVIEW_LEN)}")
        val taskId = json.decodeFromString<YouCamTaskResponse>(taskBody).data.taskId
        log("task: created task_id=$taskId")
        return taskId
    }

    private suspend fun pollTask(token: String, feature: Feature, taskId: String): String {
        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            val response = request(
                "$BASE_URL/s2s/${feature.version}/task/${feature.type}/$taskId",
                method = HttpMethod.GET,
            ) { authorized(token) }
            ensureSuccess(response, "task/poll")
            val pollBody = response.bodyAsText()
            log("poll: attempt ${attempt + 1} body=${pollBody.take(BODY_PREVIEW_LEN)}")
            val result = json.decodeFromString<YouCamPollResponse>(pollBody).data
            when (result.status.lowercase()) {
                STATUS_SUCCESS ->
                    return result.results?.firstOrNull()?.data?.firstOrNull()?.url
                        ?: error("Try-on finished but returned no image.")
                STATUS_ERROR, STATUS_FAILED -> error("Try-on failed. Please try again.")
                else -> delay(POLL_INTERVAL_MILLIS)
            }
        }
        error("Try-on timed out. Please try again.")
    }

    private suspend fun downloadResult(url: String): ByteArray {
        val response = request(url, method = HttpMethod.GET)
        ensureSuccess(response, "download")
        return response.readRawBytes()
    }

    private suspend fun request(
        url: String,
        method: HttpMethod = HttpMethod.POST,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = try {
        when (method) {
            HttpMethod.GET -> httpClient.get(url, block)
            HttpMethod.POST -> httpClient.post(url, block)
            HttpMethod.PUT -> httpClient.put(url, block)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IllegalStateException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        log("HTTP $method $url failed: ${e::class.simpleName}: ${e.message}")
        error("Network error. Check your connection.")
    }

    private fun HttpRequestBuilder.authorized(token: String) {
        header("Authorization", "Bearer $token")
    }

    private suspend fun ensureSuccess(response: HttpResponse, step: String) {
        if (response.status.isSuccess()) return
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        log("$step: HTTP ${response.status.value} body=$body")
        val message = when (response.status.value) {
            HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> "Invalid YouCam credentials. Check them in Settings."
            HTTP_TOO_MANY_REQUESTS -> "Too many requests. Please wait and try again."
            in HTTP_SERVER_ERROR_RANGE -> "YouCam service error. Please try again later."
            else -> "YouCam error ${response.status.value}: ${body.take(BODY_PREVIEW_LEN)}"
        }
        error(message)
    }

    private fun log(message: String) {
        println("[$LOG_TAG] $message")
    }

    private enum class HttpMethod { GET, POST, PUT }

    /** Which YouCam endpoint family serves a garment category. */
    private data class Feature(val version: String, val type: String)

    private fun GarmentCategory.feature(): Feature = when (this) {
        GarmentCategory.TOP, GarmentCategory.BOTTOM, GarmentCategory.FULL_BODY ->
            Feature(version = "v2.0", type = "cloth-v3")
        GarmentCategory.SHOES -> Feature(version = "v1.0", type = "shoes")
    }

    private fun GarmentCategory.garmentCategory(): String? = when (this) {
        GarmentCategory.TOP -> "upper_body"
        GarmentCategory.BOTTOM -> "lower_body"
        GarmentCategory.FULL_BODY -> "full_body"
        GarmentCategory.SHOES -> null
    }

    private companion object {
        const val LOG_TAG = "YouCam"
        const val BODY_PREVIEW_LEN = 300
        const val BASE_URL = "https://yce-api-01.perfectcorp.com"
        const val CONTENT_TYPE_JPEG = "image/jpeg"
        const val TOKEN_TTL_MILLIS = 2 * 60 * 60 * 1000L
        const val TOKEN_REFRESH_MARGIN_MILLIS = 5 * 60 * 1000L
        const val POLL_INTERVAL_MILLIS = 2000L
        const val MAX_POLL_ATTEMPTS = 60
        const val STATUS_SUCCESS = "success"
        const val STATUS_ERROR = "error"
        const val STATUS_FAILED = "failed"
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY_REQUESTS = 429
        val HTTP_SERVER_ERROR_RANGE = 500..599
    }
}
