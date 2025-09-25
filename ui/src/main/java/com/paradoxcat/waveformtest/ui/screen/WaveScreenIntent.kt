package com.paradoxcat.waveformtest.ui.screen

import android.net.Uri

sealed interface WaveScreenIntent {
    data object PickFileClicked : WaveScreenIntent
    data class FileSelected(val uri: Uri) : WaveScreenIntent
    data object PlayPauseClicked : WaveScreenIntent
    data class SeekTo(val positionFraction: Float) : WaveScreenIntent
    data object ToggleDynamicNormalization : WaveScreenIntent
    data object ClearErrorMessage : WaveScreenIntent
    data class TargetSegmentsChanged(val newSegmentCount: Int) : WaveScreenIntent // ADDED
}
