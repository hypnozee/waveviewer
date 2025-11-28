package com.waveformtest.domain.model

/**
 * Holds waveform data and total duration.
 */
data class WaveformResultData(
    val waveformSegments: List<WaveformSegment>,
    val durationMillis: Int,
)
