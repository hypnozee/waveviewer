package com.paradoxcat.waveformtest.domain.player.repository

import android.net.Uri
import com.paradoxcat.waveformtest.domain.player.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for an audio player.
 */
interface AudioPlayer {

    val playbackState: StateFlow<PlaybackState>
    suspend fun load(uri: Uri)
    fun play()
    fun pause()
    fun seekTo(positionMillis: Long)
    fun stop()
    fun release()
}
