package com.waveform.data.player

import android.app.Application
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.waveform.domain.player.model.PlaybackState
import com.waveform.domain.player.repository.AudioPlayer
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

class AudioPlayerImpl(
    private val application: Application,
    private val playerFactory: (Application) -> ExoPlayer = { app ->
        ExoPlayer.Builder(app).build()
    },
) : AudioPlayer {

    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var trackerJob: Job? = null
    private val _playbackState = MutableStateFlow(PlaybackState.default)

    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    override suspend fun load(uriString: String) = withContext(Dispatchers.Main) {
        releaseCurrentPlayer()
        _playbackState.update {
            PlaybackState.default.copy(
                currentUriString = uriString,
                isLoading = true
            )
        }

        val exoPlayer = playerFactory(application)
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val duration = exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                        _playbackState.update {
                            it.copy(totalDurationMillis = duration, isLoading = false, error = null)
                        }
                    }

                    Player.STATE_ENDED -> {
                        _playbackState.update {
                            it.copy(
                                isPlaying = false,
                                currentPositionMillis = it.totalDurationMillis
                            )
                        }
                        stopPositionTracker()
                    }

                    Player.STATE_BUFFERING -> {
                        _playbackState.update { it.copy(isLoading = true) }
                    }

                    Player.STATE_IDLE -> {
                        // Player is stopped or not yet prepared; no state update needed here
                        // as stop()/release() manage state transitions explicitly.
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionTracker() else stopPositionTracker()
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.update {
                    it.copy(
                        error = "Player error: ${error.message}",
                        isLoading = false,
                        isPlaying = false
                    )
                }
                releaseCurrentPlayer()
            }
        })

        exoPlayer.setMediaItem(MediaItem.fromUri(uriString.toUri()))
        exoPlayer.prepare()
        player = exoPlayer
    }

    override fun play() {
        player?.let {
            if (_playbackState.value.totalDurationMillis > 0) {
                if (it.playbackState == Player.STATE_ENDED) {
                    it.seekTo(0)
                }
                it.play()
            }
        } ?: _playbackState.update { it.copy(error = "Player not initialized. Call load() first.") }
    }

    override fun pause() {
        player?.pause()
    }

    override fun seekTo(positionMillis: Long) {
        player?.let {
            if (_playbackState.value.totalDurationMillis > 0) {
                val clamped = positionMillis.coerceIn(0L, _playbackState.value.totalDurationMillis)
                it.seekTo(clamped)
                _playbackState.update { state -> state.copy(currentPositionMillis = clamped) }
            }
        }
    }

    override fun stop() {
        player?.run {
            stop()
            clearMediaItems()
        }
        stopPositionTracker()
        _playbackState.update { PlaybackState.default.copy(currentUriString = it.currentUriString) }
    }

    override fun release() {
        releaseCurrentPlayer()
        _playbackState.value = PlaybackState.default
    }

    private fun releaseCurrentPlayer() {
        player?.release()
        player = null
        stopPositionTracker()
    }

    private fun startPositionTracker() {
        stopPositionTracker()
        trackerJob = scope.launch {
            while (isActive && player?.isPlaying == true) {
                val pos = player?.currentPosition ?: _playbackState.value.currentPositionMillis
                _playbackState.update { it.copy(currentPositionMillis = pos) }
                delay(50)
            }
        }
    }

    private fun stopPositionTracker() {
        trackerJob?.cancel()
        trackerJob = null
    }
}
