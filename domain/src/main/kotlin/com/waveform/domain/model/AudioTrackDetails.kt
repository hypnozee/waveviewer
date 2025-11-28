package com.waveform.domain.model

/**
 * Audio track name.
 */
data class AudioTrackDetails(
    val fileName: String?,
    val totalDurationMillis: Long = 0L,
    val sampleRate: Int = 0
)
