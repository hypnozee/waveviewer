package com.waveform.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the `public.audio_files` table.
 *
 * Matches the Supabase schema:
 * - id (uuid), name (text), mime_type (text), size_bytes (bigint),
 *   bucket_id (text), storage_path (text), user_id (uuid?), uploaded_at (timestamptz)
 */
@Serializable
data class AudioFileDto(
    val id: String,
    val name: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("bucket_id") val bucketId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String,
)
