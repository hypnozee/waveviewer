package com.waveform.domain.model

/**
 * Holds format information extracted from a WAV file's header.
 * This data is extracted from the "fmt " chunk and the "data" chunk's header.
 */
data class WavFormatInfo(
    val channels: Int,
    val sampleRate: Int,
    val bitDepth: Int,
    val dataChunkDeclaredSize: Long,
)
