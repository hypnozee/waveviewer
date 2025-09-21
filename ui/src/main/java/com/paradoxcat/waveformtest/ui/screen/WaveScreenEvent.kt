package com.paradoxcat.waveformtest.ui.screen

import android.net.Uri

sealed class WaveScreenEvent {
    data object PickFileClicked : WaveScreenEvent()
    data class FileSelected(val uri: Uri) : WaveScreenEvent()
    data object PlayPauseClicked : WaveScreenEvent()
    data class SeekTo(val positionFraction: Float) : WaveScreenEvent()
    data object ErrorMessageShown : WaveScreenEvent()
    data object ToggleDynamicNormalization : WaveScreenEvent()
}
