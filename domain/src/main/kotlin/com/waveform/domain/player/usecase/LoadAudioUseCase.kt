package com.waveform.domain.player.usecase

import com.waveform.domain.player.repository.AudioPlayer

/**
 * Use case for loading an audio file for playback.
 */
class LoadAudioUseCase(private val audioPlayer: AudioPlayer) {

    /**
     * Loads the audio file from the given URI string.
     */
    suspend operator fun invoke(uriString: String) {
        audioPlayer.load(uriString)
    }
}
