package com.waveform.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Request DTOs for Edge Function calls.
 * Each matches the expected JSON body for its corresponding function.
 *
 * The server expects camelCase keys for these request bodies.
 */

@Serializable
data class UploadRequest(
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val fileData: String,
)

@Serializable
data class DeleteRequest(val id: String)

@Serializable
data class DownloadRequest(
    val bucketId: String,
    val storagePath: String,
)
