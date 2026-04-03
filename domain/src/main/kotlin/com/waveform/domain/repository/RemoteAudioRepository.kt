package com.waveform.domain.repository

import com.waveform.domain.model.AudioFileInfo
import com.waveform.domain.core.Result

/**
 * Interface for accessing audio file records from the remote backend.
 *
 * Each operation is a call to a backend API endpoint.
 * The app does not query the database directly — all access control
 * and storage logic is handled server-side.
 */
interface RemoteAudioRepository {

    /**
     * Fetch all publicly available audio files.
     */
    suspend fun getPublicAudioFiles(): Result<List<AudioFileInfo>>

    /**
     * Fetch audio files owned by the current authenticated user.
     */
    suspend fun getUserAudioFiles(): Result<List<AudioFileInfo>>

    /**
     * Upload an audio file.
     */
    suspend fun uploadAudioFile(
        name: String,
        mimeType: String,
        bytes: ByteArray,
    ): Result<AudioFileInfo>

    /**
     * Delete an audio file by ID.
     */
    suspend fun deleteAudioFile(id: String): Result<Unit>

    /**
     * Download the raw audio bytes for a file.
     */
    suspend fun downloadAudioFile(
        bucketId: String,
        storagePath: String,
    ): Result<ByteArray>
}
