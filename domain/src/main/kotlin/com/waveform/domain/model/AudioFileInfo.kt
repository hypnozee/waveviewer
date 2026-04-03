package com.waveform.domain.model

/**
 * Domain model representing an audio file record from the backend.
 */
data class AudioFileInfo(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val bucketId: String,
    val storagePath: String,
    val userId: String?,
    val uploadedAt: String,
)
