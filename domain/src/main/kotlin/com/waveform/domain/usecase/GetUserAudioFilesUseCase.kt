package com.waveform.domain.usecase

import com.waveform.domain.core.Result
import com.waveform.domain.model.AudioFileInfo
import com.waveform.domain.repository.RemoteAudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetUserAudioFilesUseCase(private val remoteAudioRepository: RemoteAudioRepository) {

    suspend operator fun invoke(): Result<List<AudioFileInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                remoteAudioRepository.getUserAudioFiles()
            } catch (e: Exception) {
                Result.Error("Failed to fetch user audio files: ${e.message}")
            }
        }
    }
}
