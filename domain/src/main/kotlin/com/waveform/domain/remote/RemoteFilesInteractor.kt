package com.waveform.domain.remote

import com.waveform.domain.core.Result
import com.waveform.domain.model.AudioFileInfo
import com.waveform.domain.repository.RemoteAudioRepository

/**
 * Interactor that groups all remote file related operations.
 */
class RemoteFilesInteractor(
    private val remoteAudioRepository: RemoteAudioRepository
) {
    suspend fun getPublicAudioFiles(): Result<List<AudioFileInfo>> =
        remoteAudioRepository.getPublicAudioFiles()

    suspend fun getUserAudioFiles(): Result<List<AudioFileInfo>> =
        remoteAudioRepository.getUserAudioFiles()

    suspend fun uploadAudioFile(fileName: String, mimeType: String, bytes: ByteArray): Result<AudioFileInfo> =
        remoteAudioRepository.uploadAudioFile(fileName, mimeType, bytes)

    suspend fun downloadAudioFile(bucketId: String, storagePath: String): Result<ByteArray> =
        remoteAudioRepository.downloadAudioFile(bucketId, storagePath)

    suspend fun deleteAudioFile(id: String): Result<Unit> =
        remoteAudioRepository.deleteAudioFile(id)
}
