package com.waveformtest.data.player

import android.app.Application
import android.media.MediaPlayer
import androidx.core.net.toUri
import com.waveformtest.domain.player.model.PlaybackState
import com.waveformtest.domain.player.repository.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Plays audio using Android's MediaPlayer.
 * It can load, play, pause, seek, and stop audio.
 * It also keeps track of the playback status.
 **/
class AudioPlayerImpl(
    private val application: Application,
) : AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionTrackerJob: Job? = null
    private val _playbackState = MutableStateFlow(PlaybackState.default)

    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    override suspend fun load(uriString: String) = withContext(Dispatchers.IO) {
        releaseCurrentPlayer()
        _playbackState.update {
            PlaybackState.default.copy(currentUriString = uriString, isLoading = true)
        }
        val uri = uriString.toUri() // Convert String to Uri
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(application, uri)
                prepareAsync()

                setOnPreparedListener { mp ->
                    coroutineScope.launch(Dispatchers.Main) {
                        _playbackState.update {
                            it.copy(
                                totalDurationMillis = mp.duration.toLong(),
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                }
                setOnCompletionListener {
                    coroutineScope.launch(Dispatchers.Main) {
                        _playbackState.update {
                            it.copy(
                                isPlaying = false,
                                currentPositionMillis = it.totalDurationMillis
                            )
                        }
                        stopPositionTracker()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    coroutineScope.launch(Dispatchers.Main) {
                        _playbackState.update {
                            it.copy(
                                error = "MediaPlayer Error - What: $what, Extra: $extra",
                                isLoading = false,
                                isPlaying = false
                            )
                        }
                        releaseCurrentPlayer()
                    }
                    true
                }
            } catch (e: Exception) {
                coroutineScope.launch(Dispatchers.Main) {
                    _playbackState.update {
                        it.copy(
                            error = "Failed to initialize media player: ${e.message}",
                            isLoading = false,
                            isPlaying = false
                        )
                    }
                    releaseCurrentPlayer()
                }
            }
        }
    }

    override fun play() {
        mediaPlayer?.let {
            if (!it.isPlaying && playbackState.value.totalDurationMillis > 0 && !playbackState.value.isLoading) {
                try {
                    it.start()
                    _playbackState.update { state -> state.copy(isPlaying = true, error = null) }
                    startPositionTracker()
                } catch (e: IllegalStateException) {
                    _playbackState.update { state ->
                        state.copy(
                            error = "MediaPlayer error on play: ${e.message}",
                            isPlaying = false
                        )
                    }
                }
            }
        } ?: run {
            _playbackState.update { it.copy(error = "Player not initialized. Call load() first.") }
        }
    }

    override fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                try {
                    it.pause()
                    _playbackState.update { state -> state.copy(isPlaying = false) }
                    stopPositionTracker()
                } catch (e: IllegalStateException) {
                    _playbackState.update { state ->
                        state.copy(error = "MediaPlayer error on pause: ${e.message}")
                    }
                }
            }
        }
    }

    override fun seekTo(positionMillis: Long) {
        mediaPlayer?.let {
            if (playbackState.value.totalDurationMillis > 0) {
                try {
                    val newPosition = positionMillis.coerceIn(0, it.duration.toLong())
                    it.seekTo(newPosition.toInt())
                    _playbackState.update { state -> state.copy(currentPositionMillis = newPosition) }
                } catch (e: IllegalStateException) {
                    _playbackState.update { state ->
                        state.copy(error = "MediaPlayer error on seek: ${e.message}")
                    }
                }
            }
        }
    }

    override fun stop() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                _playbackState.update {
                    PlaybackState.default.copy(currentUriString = it.currentUriString)
                }
            } catch (e: IllegalStateException) {
                _playbackState.update { state ->
                    state.copy(error = "MediaPlayer error on stop: ${e.message}")
                }
            }
        }
        stopPositionTracker()
    }

    private fun releaseCurrentPlayer() {
        mediaPlayer?.apply {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            try {
                if (isPlaying) {
                    this.stop()
                }
                reset()
                release()
            } catch (e: Exception) {
                _playbackState.update {
                    it.copy(error = "Error releasing existing media player: ${e.message}")
                }
            }
        }
        mediaPlayer = null
        stopPositionTracker()
    }

    override fun release() {
        releaseCurrentPlayer()
        _playbackState.value = PlaybackState.default
    }


    private fun startPositionTracker() {
        stopPositionTracker()
        positionTrackerJob = coroutineScope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                try {
                    val currentPosition = mediaPlayer?.currentPosition?.toLong()
                        ?: playbackState.value.currentPositionMillis
                    _playbackState.update { it.copy(currentPositionMillis = currentPosition) }
                } catch (e: IllegalStateException) {
                    _playbackState.update { it.copy(error = "Error updating position: ${e.message}") }
                    stopPositionTracker()
                    break
                }
                delay(50)
            }
        }
    }

    private fun stopPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = null
    }
}
