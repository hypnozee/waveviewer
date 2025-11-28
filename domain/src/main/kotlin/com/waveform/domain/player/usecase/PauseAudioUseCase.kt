package com.waveform.domain.player.usecase

import com.waveform.domain.player.repository.AudioPlayer

/**
 * Use case for pausing the currently playing audio.
 */
class PauseAudioUseCase(private val audioPlayer: AudioPlayer) {

    /**
     * Pause the audio playback.
     */
    operator fun invoke() {
        audioPlayer.pause()
    }
}
