package com.waveform.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the audio files.
 *
 * The server returns camelCase keys.
 */
@Serializable
data class AudioFileDto(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val bucketId: String,
    val storagePath: String,
    val userId: String? = null,
    val uploadedAt: String,
)
