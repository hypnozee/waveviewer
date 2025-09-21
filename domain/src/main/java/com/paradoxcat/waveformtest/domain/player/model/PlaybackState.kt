package com.paradoxcat.waveformtest.domain.player.model

/**
 * Represents the current state of media playback.
 */
data class PlaybackState(
    val currentUriString: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    companion object {
        val default = PlaybackState()
    }
}
