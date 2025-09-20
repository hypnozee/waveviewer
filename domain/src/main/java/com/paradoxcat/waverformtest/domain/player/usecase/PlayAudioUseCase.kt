package com.paradoxcat.waverformtest.domain.player.usecase

import com.paradoxcat.waverformtest.domain.player.repository.AudioPlayer

/**
 * Use case for starting or resuming audio playback.
 */
class PlayAudioUseCase(private val audioPlayer: AudioPlayer) {

    /**
     * Starts or resumes playback.
     */
    operator fun invoke() {
        audioPlayer.play()
    }
}
