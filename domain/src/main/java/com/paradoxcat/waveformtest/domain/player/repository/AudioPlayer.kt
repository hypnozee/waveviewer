package com.paradoxcat.waveformtest.domain.player.repository

import com.paradoxcat.waveformtest.domain.player.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for an audio player.
 */
interface AudioPlayer {

    val playbackState: StateFlow<PlaybackState>
    suspend fun load(uriString: String)
    fun play()
    fun pause()
    fun seekTo(positionMillis: Long)
    fun stop()
    fun release()
}
