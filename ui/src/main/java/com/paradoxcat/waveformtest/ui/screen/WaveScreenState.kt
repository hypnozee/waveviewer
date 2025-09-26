package com.paradoxcat.waveformtest.ui.screen

import android.net.Uri
import com.paradoxcat.waveformtest.domain.model.WaveformSegment

const val DEFAULT_TARGET_SEGMENTS = 500
const val MIN_TARGET_SEGMENTS = 50
const val MAX_TARGET_SEGMENTS = 1000

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
    val currentAudioPosition: String = "00:00",
    val totalAudioDuration: String = "00:00",
    val errorMessage: String? = null,
    val dynamicNormalizationEnabled: Boolean = false,
    val currentTargetSegments: Int = DEFAULT_TARGET_SEGMENTS,
)
