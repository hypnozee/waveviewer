package com.paradoxcat.waverformtest.domain.player.usecase

import com.paradoxcat.waverformtest.domain.player.model.PlaybackState
import com.paradoxcat.waverformtest.domain.player.repository.AudioPlayer
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for observing the playback state of the audio player.
 */
class ObservePlaybackStateUseCase(private val audioPlayer: AudioPlayer) {

    /**
     * PlayabckState is something we need to know reactively/observably
     */
    operator fun invoke(): StateFlow<PlaybackState> {
        return audioPlayer.playbackState
    }
}
