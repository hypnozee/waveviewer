package com.waveform.domain.usecase

import com.waveform.domain.model.AudioTrackDetails
import com.waveform.domain.repository.AudioRepository
import com.waveform.domain.core.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetAudioTrackDetailsUseCase(private val audioRepository: AudioRepository) {

    suspend operator fun invoke(uriString: String): Result<AudioTrackDetails> {
        return withContext(Dispatchers.IO) {
            try {
                val trackDetails = audioRepository.getAudioTrackDetails(uriString)
                if (trackDetails != null) {
                    Result.Success(trackDetails)
                } else {
                    Result.Error("Could not retrieve track details for the given URI string.")
                }
            } catch (e: Exception) {
                Result.Error("Failed to get track details: ${e.message}")
            }
        }
    }
}
