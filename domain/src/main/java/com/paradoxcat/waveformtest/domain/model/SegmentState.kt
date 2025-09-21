package com.paradoxcat.waveformtest.domain.model

/**
 * For each segment, it stores the min and max audio sample values,
 * if the segment has any audio data, and how many  audio samples were combined.
 */
data class SegmentState(
    val minValues: FloatArray,
    val maxValues: FloatArray,
    val populated: BooleanArray,
    val samplesPerSegment: Double,
)
