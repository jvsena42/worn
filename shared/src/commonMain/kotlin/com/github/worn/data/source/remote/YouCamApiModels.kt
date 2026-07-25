package com.github.worn.data.source.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Auth ------------------------------------------------------------------------------------------

@Serializable
internal data class YouCamAuthRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("id_token") val idToken: String,
)

@Serializable
internal data class YouCamAuthResponse(val result: Result) {
    @Serializable
    data class Result(@SerialName("access_token") val accessToken: String)
}

// File upload -----------------------------------------------------------------------------------

@Serializable
internal data class YouCamFileRequest(val files: List<Spec>) {
    @Serializable
    data class Spec(
        @SerialName("content_type") val contentType: String,
        @SerialName("file_name") val fileName: String,
        @SerialName("file_size") val fileSize: Int,
    )
}

@Serializable
internal data class YouCamFileResponse(val result: Result) {
    @Serializable
    data class Result(val files: List<Entry>)

    @Serializable
    data class Entry(
        @SerialName("file_id") val fileId: String,
        val requests: List<UploadRequest>,
    )

    @Serializable
    data class UploadRequest(
        val url: String,
        val method: String,
        val headers: Map<String, String> = emptyMap(),
    )
}

// Task creation + polling -----------------------------------------------------------------------
// NOTE: the exact task request/response wire format should be confirmed against
// docs.perfectcorp.com/reference/ai_clothes before shipping; it is isolated here so only these
// DTOs change. The documented fields (src_file_id, ref_file_id, garment_category, task_id, status)
// are modelled below.

@Serializable
internal data class YouCamTaskRequest(
    @SerialName("src_file_id") val srcFileId: String,
    @SerialName("ref_file_id") val refFileId: String,
    @SerialName("garment_category") val garmentCategory: String? = null,
)

@Serializable
internal data class YouCamTaskResponse(val result: Result) {
    @Serializable
    data class Result(@SerialName("task_id") val taskId: String)
}

@Serializable
internal data class YouCamPollResponse(val result: Result) {
    @Serializable
    data class Result(
        val status: String,
        @SerialName("results") val outputs: List<Output> = emptyList(),
        val error: String? = null,
    )

    @Serializable
    data class Output(val url: String)
}

// Errors ----------------------------------------------------------------------------------------

@Serializable
internal data class YouCamErrorResponse(
    val error: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
)
