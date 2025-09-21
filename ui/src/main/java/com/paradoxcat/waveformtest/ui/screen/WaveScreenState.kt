package com.paradoxcat.waveformtest.ui.screen

import android.net.Uri
import com.paradoxcat.waveformtest.domain.model.WaveformSegment

data class WaveScreenState(
    val isLoadingFile: Boolean = false,
    val isLoadingWaveform: Boolean = false,
    val isPlayerLoading: Boolean = false,
    val fileName: String? = null,
    val fileUri: Uri? = null,
    val waveformData: List<WaveformSegment>? = null,
    val isPlaying: Boolean = false,
    val currentPositionMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val errorMessage: String? = null,
    val dynamicNormalizationEnabled: Boolean = false,
)
