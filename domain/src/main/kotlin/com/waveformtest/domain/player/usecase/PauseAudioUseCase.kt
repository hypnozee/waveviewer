package com.waveformtest.domain.player.usecase

import com.waveformtest.domain.player.repository.AudioPlayer

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
