package com.paradoxcat.waveformtest.domain.player.usecase

import com.paradoxcat.waveformtest.domain.player.repository.AudioPlayer

/**
 * Use case for "cleaning up" the audio player + resources.
 */
class ReleasePlayerUseCase(private val audioPlayer: AudioPlayer) {

    /**
     * Releases the audio player.
     */
    operator fun invoke() {
        audioPlayer.release()
    }
}
