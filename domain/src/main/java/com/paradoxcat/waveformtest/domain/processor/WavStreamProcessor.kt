package com.paradoxcat.waveformtest.domain.processor

import com.paradoxcat.waveformtest.domain.core.Result
import com.paradoxcat.waveformtest.domain.model.WavFormatInfo
import com.paradoxcat.waveformtest.domain.model.WaveformResultData
import com.paradoxcat.waveformtest.domain.model.WaveformSegment
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

object WavStreamProcessor {
    private const val EXPECTED_CHANNELS = 1
    private const val EXPECTED_BIT_DEPTH = 16
    private const val TARGET_SEGMENTS = 750
    private const val STREAM_READ_BUFFER_SIZE = 4096
    private const val MAX_16_BIT = 32768.0f

    fun processStream(
        inputStream: InputStream,
        formatInfo: WavFormatInfo,
    ): Result<WaveformResultData> {
        try {
            // validate 16 bit, PCM, mono
            if (formatInfo.channels != EXPECTED_CHANNELS) {
                return Result.Error("Unsupported number of channels: ${formatInfo.channels}. Expected $EXPECTED_CHANNELS for mono.")
            }
            if (formatInfo.bitDepth != EXPECTED_BIT_DEPTH) {
                return Result.Error("Unsupported bit depth: ${formatInfo.bitDepth}. Expected $EXPECTED_BIT_DEPTH.")
            }

            val bytesPerSample = EXPECTED_BIT_DEPTH / 8 // Should be 2
            // For mono, bytesPerFrame is the same as bytesPerSample
            val bytesPerFrame = bytesPerSample * EXPECTED_CHANNELS // Should be 2

            if (formatInfo.dataChunkDeclaredSize == 0L) {
                return Result.Success(
                    data = WaveformResultData(
                        waveformSegments = emptyList(),
                        durationMillis = 0
                    )
                )
            }
            if (formatInfo.sampleRate <= 0) {
                return Result.Error("Invalid sampleRate: ${formatInfo.sampleRate}")
            }

            val totalExpectedSamples = formatInfo.dataChunkDeclaredSize / bytesPerFrame
            if (totalExpectedSamples <= 0) {
                // This case should be covered by dataChunkDeclaredSize == 0, but for extra safety:
                return Result.Success(
                    WaveformResultData(
                        waveformSegments = emptyList(),
                        durationMillis = 0
                    )
                )
            }

            val samplesPerTargetSegment = totalExpectedSamples.toDouble() / TARGET_SEGMENTS

            val minValues = FloatArray(TARGET_SEGMENTS) { Float.POSITIVE_INFINITY }
            val maxValues = FloatArray(TARGET_SEGMENTS) { Float.NEGATIVE_INFINITY }
            val segmentPopulated = BooleanArray(TARGET_SEGMENTS) { false }

            val readBuffer = ByteArray(STREAM_READ_BUFFER_SIZE)
            var totalBytesSuccessfullyReadFromStream = 0L
            var currentOverallSampleIndex = 0L

            while (totalBytesSuccessfullyReadFromStream < formatInfo.dataChunkDeclaredSize) {
                val bytesRemainingInChunk =
                    formatInfo.dataChunkDeclaredSize - totalBytesSuccessfullyReadFromStream
                val bytesToReadForThisPass =
                    min(readBuffer.size.toLong(), bytesRemainingInChunk).toInt()

                if (bytesToReadForThisPass == 0) break

                val actualBytesReadThisPass =
                    inputStream.read(readBuffer, 0, bytesToReadForThisPass)

                if (actualBytesReadThisPass == -1) {
                    if (totalBytesSuccessfullyReadFromStream == 0L && bytesRemainingInChunk > 0) {
                        return Result.Error("Reached EOF at the start of the data chunk when data was expected.")
                    }
                    break
                }
                if (actualBytesReadThisPass == 0) break

                totalBytesSuccessfullyReadFromStream += actualBytesReadThisPass

                // Process only whole frames/samples
                val processableBytesInThisBuffer =
                    (actualBytesReadThisPass / bytesPerFrame) * bytesPerFrame
                val samplesInThisBuffer = processableBytesInThisBuffer / bytesPerFrame

                for (i in 0 until samplesInThisBuffer) {
                    val sampleOffsetInReadBuffer = i * bytesPerFrame
                    val sampleByteBuffer =
                        ByteBuffer.wrap(readBuffer, sampleOffsetInReadBuffer, bytesPerSample)
                            .order(ByteOrder.LITTLE_ENDIAN)

                    val rawSample = sampleByteBuffer.short.toInt()
                    val normalizedSample = rawSample / MAX_16_BIT

                    val targetSegmentIndex = if (samplesPerTargetSegment > 0) {
                        (currentOverallSampleIndex / samplesPerTargetSegment).toInt()
                    } else {
                        // For such very short files, map each sample to its own segment.
                        // We might still produce fewer than TARGET_SEGMENTS overall.
                        currentOverallSampleIndex.toInt()
                    }.coerceIn(0, TARGET_SEGMENTS - 1)

                    minValues[targetSegmentIndex] =
                        min(minValues[targetSegmentIndex], normalizedSample)
                    maxValues[targetSegmentIndex] =
                        max(maxValues[targetSegmentIndex], normalizedSample)
                    segmentPopulated[targetSegmentIndex] = true

                    currentOverallSampleIndex++
                }

                if (actualBytesReadThisPass < bytesToReadForThisPass) {
                    break
                }
            }

            val segments = mutableListOf<WaveformSegment>()
            for (i in 0 until TARGET_SEGMENTS) {
                segments.add(WaveformSegment(minValues[i], maxValues[i]))
            }

            if (segments.isEmpty() && currentOverallSampleIndex > 0) {
                // This indicates an issue if samples were processed but no segments were populated.
                println("Warning: No segments populated despite processing $currentOverallSampleIndex samples.")
            }

            // Duration should be based on the actual number of samples processed.
            val actualTotalSamplesProcessed = currentOverallSampleIndex
            val durationMillis = if (actualTotalSamplesProcessed > 0) {
                (actualTotalSamplesProcessed * 1000L / formatInfo.sampleRate).toInt()
            } else {
                0
            }

            return Result.Success(WaveformResultData(segments, durationMillis))

        } catch (e: IOException) {
            return Result.Error("IOException during WAV stream processing: ${e.message}")
        } catch (e: Exception) {
            return Result.Error("Unexpected error during WAV stream processing: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}
