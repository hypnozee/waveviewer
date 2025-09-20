package com.paradoxcat.waverformtest.domain.player.usecase

import com.paradoxcat.waverformtest.domain.player.repository.AudioPlayer

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
