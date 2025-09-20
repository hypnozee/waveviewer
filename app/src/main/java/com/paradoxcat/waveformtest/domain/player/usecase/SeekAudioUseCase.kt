package com.paradoxcat.waveformtest.domain.player.usecase

import com.paradoxcat.waveformtest.domain.player.repository.AudioPlayer

/**
 * Use case for jumping to a part of the audio.
 */
class SeekAudioUseCase(private val audioPlayer: AudioPlayer) {

    /**
     * Jump to positionMillis
     */
    operator fun invoke(positionMillis: Long) {
        audioPlayer.seekTo(positionMillis)
    }
}
