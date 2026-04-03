package com.waveform.domain.usecase

import com.waveform.domain.core.Result
import com.waveform.domain.repository.RemoteAudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteAudioFileUseCase(private val remoteAudioRepository: RemoteAudioRepository) {

    suspend operator fun invoke(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                remoteAudioRepository.deleteAudioFile(id)
            } catch (e: Exception) {
                Result.Error("Failed to delete audio file: ${e.message}")
            }
        }
    }
}
