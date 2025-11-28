package com.waveform.domain.model

/**
 * For each segment, it stores the min and max audio sample values,
 * if the segment has any audio data,
 * and how many  audio samples were combined.
 */
data class SegmentState(
    val minValues: FloatArray,
    val maxValues: FloatArray,
    val populated: BooleanArray,
    val samplesPerSegment: Double,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SegmentState

        if (samplesPerSegment != other.samplesPerSegment) return false
        if (!minValues.contentEquals(other.minValues)) return false
        if (!maxValues.contentEquals(other.maxValues)) return false
        if (!populated.contentEquals(other.populated)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = samplesPerSegment.hashCode()
        result = 31 * result + minValues.contentHashCode()
        result = 31 * result + maxValues.contentHashCode()
        result = 31 * result + populated.contentHashCode()
        return result
    }
}
