package com.paradoxcat.waveformtest.domain.parser

import com.paradoxcat.waveformtest.domain.model.WavFormatInfo
import java.io.InputStream

/**
 * A helper for reading the header of WAV audio files.
 * It looks for important parts like "RIFF", "WAVE", "fmt ", and "data"
 */
object WavHeaderParser {

    /**
     * NOT YET IMPLEMENTED !
     */
    fun parseWavHeaderAndLocateDataChunk(
        inputStream: InputStream,
        uriString: String,
    ): WavFormatInfo? {
        return null
    }
}
