package com.paradoxcat.waveformtest.domain.processor

import com.paradoxcat.waveformtest.domain.core.Result
import com.paradoxcat.waveformtest.domain.model.WavFormatInfo
import com.paradoxcat.waveformtest.domain.model.WaveformResultData
import java.io.InputStream

object WavStreamProcessor {

    fun processStream(
        inputStream: InputStream,
        formatInfo: WavFormatInfo,
    ): Result<WaveformResultData> {
        return Result.Error("WORK IN PROGRESS - not ready yet.")
    }
}
