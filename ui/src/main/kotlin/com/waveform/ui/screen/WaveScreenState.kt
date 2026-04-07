package com.waveform.ui.screen

import android.net.Uri
import com.waveform.domain.model.AuthState
import com.waveform.domain.model.WaveformSegment

const val DEFAULT_SEGMENTS_NUMBER = 500
const val MIN_NUM_SEGMENTS = 50
const val MAX_NUM_SEGMENTS = 1000

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
    val isSeeking: Boolean = false,
    val errorMessage: String? = null,
    val dynamicNormalizationEnabled: Boolean = false,
    val currentNumSegments: Int = DEFAULT_SEGMENTS_NUMBER,
    val displayMinAmplitude: Float = -1.0f,
    val displayMaxAmplitude: Float = 1.0f,
    val authState: AuthState = AuthState.Loading,
)
