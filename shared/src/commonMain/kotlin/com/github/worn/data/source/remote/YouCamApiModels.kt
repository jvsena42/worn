package com.github.worn.data.source.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Field names confirmed against live cloth-v3 responses. Note the wrapper differs by endpoint:
// auth wraps its payload in "result", while file/task/poll wrap in "data".

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
internal data class YouCamFileResponse(@SerialName("data") val data: Payload) {
    @Serializable
    data class Payload(val files: List<Entry>)

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

@Serializable
internal data class YouCamTaskRequest(
    @SerialName("src_file_id") val srcFileId: String,
    @SerialName("ref_file_id") val refFileId: String,
    @SerialName("garment_category") val garmentCategory: String? = null,
)

@Serializable
internal data class YouCamTaskResponse(@SerialName("data") val data: Payload) {
    @Serializable
    data class Payload(@SerialName("task_id") val taskId: String)
}

// Poll: `task_status` is "running" until the task finishes as "success" / "error". While running,
// `results` is null; on success it holds one entry per output, each with a `data` array of URLs.
@Serializable
internal data class YouCamPollResponse(@SerialName("data") val data: Payload) {
    @Serializable
    data class Payload(
        @SerialName("task_status") val status: String,
        val results: List<TaskResult>? = null,
        val error: String? = null,
    )

    @Serializable
    data class TaskResult(val data: List<Output>? = null)

    @Serializable
    data class Output(val url: String)
}

// Errors ----------------------------------------------------------------------------------------
// e.g. {"status":400,"error":"...","error_code":"InvalidParameters"}

@Serializable
internal data class YouCamErrorResponse(
    val error: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
)
