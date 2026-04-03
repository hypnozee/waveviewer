package com.waveform.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTOs for Edge Function calls.
 * Each matches the expected JSON body for its corresponding function.
 */

@Serializable
data class UploadRequest(
    val name: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("file_data") val fileData: String,
)

@Serializable
data class DeleteRequest(val id: String)

@Serializable
data class DownloadRequest(
    @SerialName("bucket_id") val bucketId: String,
    @SerialName("storage_path") val storagePath: String,
)
