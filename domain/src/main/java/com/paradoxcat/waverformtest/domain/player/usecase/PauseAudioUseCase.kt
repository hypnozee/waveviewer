package com.paradoxcat.waverformtest.domain.player.usecase

import com.paradoxcat.waverformtest.domain.player.repository.AudioPlayer

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
