package com.waveform.domain.player.usecase

import com.waveform.domain.player.repository.AudioPlayer

/**
 * Use case for stopping the audio playback.
 */
class StopAudioUseCase(private val audioPlayer: AudioPlayer) {

    /**
     * Stops the audio playback
     */
    operator fun invoke() {
        audioPlayer.stop()
    }
}
