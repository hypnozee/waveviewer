package com.waveform.domain.usecase

import com.waveform.domain.core.Result
import com.waveform.domain.model.AudioFileInfo
import com.waveform.domain.repository.RemoteAudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadAudioFileUseCase(private val remoteAudioRepository: RemoteAudioRepository) {

    suspend operator fun invoke(
        name: String,
        mimeType: String,
        bytes: ByteArray,
    ): Result<AudioFileInfo> {
        return withContext(Dispatchers.IO) {
            try {
                remoteAudioRepository.uploadAudioFile(name, mimeType, bytes)
            } catch (e: Exception) {
                Result.Error("Failed to upload audio file: ${e.message}")
            }
        }
    }
}
