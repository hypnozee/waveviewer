package com.waveform.domain.usecase

import com.waveform.domain.core.Result
import com.waveform.domain.repository.RemoteAudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadAudioFileUseCase(private val remoteAudioRepository: RemoteAudioRepository) {

    suspend operator fun invoke(bucketId: String, storagePath: String): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                remoteAudioRepository.downloadAudioFile(bucketId, storagePath)
            } catch (e: Exception) {
                Result.Error("Failed to download audio file: ${e.message}")
            }
        }
    }
}
