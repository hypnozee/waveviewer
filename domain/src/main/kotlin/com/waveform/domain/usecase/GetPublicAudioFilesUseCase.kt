package com.waveform.domain.usecase

import com.waveform.domain.core.Result
import com.waveform.domain.model.AudioFileInfo
import com.waveform.domain.repository.RemoteAudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetPublicAudioFilesUseCase(private val remoteAudioRepository: RemoteAudioRepository) {

    suspend operator fun invoke(): Result<List<AudioFileInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                remoteAudioRepository.getPublicAudioFiles()
            } catch (e: Exception) {
                Result.Error("Failed to fetch public audio files: ${e.message}")
            }
        }
    }
}
