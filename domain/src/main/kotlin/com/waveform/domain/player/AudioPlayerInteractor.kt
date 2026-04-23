package com.waveform.domain.player

import com.waveform.domain.player.model.PlaybackState
import com.waveform.domain.player.repository.AudioPlayer
import kotlinx.coroutines.flow.StateFlow

/**
 * Interactor that groups all audio player related operations.
 * Reduces the number of dependencies in ViewModels.
 */
class AudioPlayerInteractor(
    private val audioPlayer: AudioPlayer
) {
    val playbackState: StateFlow<PlaybackState> = audioPlayer.playbackState

    suspend fun load(uriString: String) {
        audioPlayer.load(uriString)
    }

    fun play() {
        audioPlayer.play()
    }

    fun pause() {
        audioPlayer.pause()
    }

    fun seekTo(positionMillis: Long) {
        audioPlayer.seekTo(positionMillis)
    }

    fun stop() {
        audioPlayer.stop()
    }

    fun release() {
        audioPlayer.release()
    }
}
