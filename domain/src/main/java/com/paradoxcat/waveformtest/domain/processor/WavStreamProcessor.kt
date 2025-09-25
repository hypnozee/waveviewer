package com.paradoxcat.waveformtest.domain.processor

import com.paradoxcat.waveformtest.domain.core.Result
import com.paradoxcat.waveformtest.domain.model.SegmentState
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
    private const val STREAM_READ_BUFFER_SIZE = 4096
    private const val MAX_16_BIT = 32768.0f
    private const val BYTES_PER_FRAME_MONO_16_BIT = 2 // EXPECTED_BIT_DEPTH / 8 * EXPECTED_CHANNELS

    fun processStream(
        inputStream: InputStream,
        formatInfo: WavFormatInfo,
        targetSegments: Int,
    ): Result<WaveformResultData> {
        try {
            // Initial Validations
            validateFormat(formatInfo)?.let { return it }

            if (targetSegments <= 0) {
                return Result.Error("Target segments must be greater than 0. Received: $targetSegments")
            }

            if (formatInfo.dataChunkDeclaredSize == 0L) {
                return Result.Success(WaveformResultData(emptyList(), 0))
            }

            val totalExpectedSamples =
                formatInfo.dataChunkDeclaredSize / BYTES_PER_FRAME_MONO_16_BIT
            if (totalExpectedSamples <= 0) {
                return Result.Success(WaveformResultData(emptyList(), 0))
            }

            val segmentState = initializeSegmentState(totalExpectedSamples, targetSegments)
            val readBuffer = ByteArray(STREAM_READ_BUFFER_SIZE)
            var totalBytesSuccessfullyReadFromStream = 0L
            var currentOverallSampleIndex = 0L

            // Main processing loop
            while (totalBytesSuccessfullyReadFromStream < formatInfo.dataChunkDeclaredSize) {
                val bytesToReadForThisPass = min(
                    readBuffer.size.toLong(),
                    formatInfo.dataChunkDeclaredSize - totalBytesSuccessfullyReadFromStream
                ).toInt()
                if (bytesToReadForThisPass == 0) break

                val actualBytesReadThisPass =
                    inputStream.read(readBuffer, 0, bytesToReadForThisPass)
                if (actualBytesReadThisPass == -1) { // EOF
                    if (totalBytesSuccessfullyReadFromStream == 0L) { // Simplified EOF check
                        return Result.Error("Reached EOF at the start of the data chunk when data was expected.")
                    }
                    break
                }
                if (actualBytesReadThisPass == 0) break

                totalBytesSuccessfullyReadFromStream += actualBytesReadThisPass
                currentOverallSampleIndex = processSamplesInCurrentBuffer(
                    readBuffer,
                    actualBytesReadThisPass,
                    segmentState,
                    currentOverallSampleIndex,
                    targetSegments,
                )

                if (actualBytesReadThisPass < bytesToReadForThisPass) break
            }

            val segments = createFinalSegments(segmentState, targetSegments) // Pass targetSegments
            logSegmentPopulationWarnings(segments, segmentState, currentOverallSampleIndex, targetSegments) // Pass targetSegments

            val durationMillis =
                calculateDurationMillis(currentOverallSampleIndex, formatInfo.sampleRate)
            return Result.Success(WaveformResultData(segments, durationMillis))

        } catch (e: IOException) {
            return Result.Error("IOException during WAV stream processing: ${e.message}")
        } catch (e: Exception) {
            return Result.Error("Unexpected error during WAV stream processing: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    private fun validateFormat(formatInfo: WavFormatInfo): Result<WaveformResultData>? {
        if (formatInfo.channels != EXPECTED_CHANNELS) {
            return Result.Error("Unsupported number of channels: ${formatInfo.channels}. Expected $EXPECTED_CHANNELS for mono.")
        }
        if (formatInfo.bitDepth != EXPECTED_BIT_DEPTH) {
            return Result.Error("Unsupported bit depth: ${formatInfo.bitDepth}. Expected $EXPECTED_BIT_DEPTH.")
        }
        if (formatInfo.sampleRate <= 0) {
            return Result.Error("Invalid sampleRate: ${formatInfo.sampleRate}")
        }
        return null
    }

    private fun initializeSegmentState(totalExpectedSamples: Long, targetSegments: Int): SegmentState {
        val samplesPerSegmentValue = if (targetSegments > 0) {
            totalExpectedSamples.toDouble() / targetSegments
        } else {
            0.0 // Should be caught by validation earlier, but defensive
        }
        return SegmentState(
            minValues = FloatArray(targetSegments) { MAX_16_BIT },
            maxValues = FloatArray(targetSegments) { - MAX_16_BIT },
            populated = BooleanArray(targetSegments) { false },
            samplesPerSegment = samplesPerSegmentValue
        )
    }

    private fun processSamplesInCurrentBuffer(
        buffer: ByteArray,
        bytesRead: Int,
        segmentState: SegmentState,
        startingSampleIndex: Long,
        targetSegments: Int,
    ): Long {
        var currentOverallSampleIndex = startingSampleIndex
        val samplesInThisBuffer = bytesRead / BYTES_PER_FRAME_MONO_16_BIT

        for (i in 0 until samplesInThisBuffer) {
            val sampleOffsetInReadBuffer = i * BYTES_PER_FRAME_MONO_16_BIT
            val sampleByteBuffer = ByteBuffer.wrap(
                buffer,
                sampleOffsetInReadBuffer,
                BYTES_PER_FRAME_MONO_16_BIT
            ).order(ByteOrder.LITTLE_ENDIAN)

            val rawSample = sampleByteBuffer.short.toInt()
            val normalizedSample = rawSample / MAX_16_BIT

            // Check targetSegments > 0 before division or array access
            if (targetSegments > 0) {
                val targetSegmentIndex = if (segmentState.samplesPerSegment > 0) {
                    (currentOverallSampleIndex / segmentState.samplesPerSegment).toInt()
                } else {
                    currentOverallSampleIndex.toInt().coerceIn(0, targetSegments - 1)
                }.coerceIn(0, targetSegments - 1)

                segmentState.minValues[targetSegmentIndex] =
                    min(segmentState.minValues[targetSegmentIndex], normalizedSample)
                segmentState.maxValues[targetSegmentIndex] =
                    max(segmentState.maxValues[targetSegmentIndex], normalizedSample)
                segmentState.populated[targetSegmentIndex] = true
            }
            currentOverallSampleIndex++
        }
        return currentOverallSampleIndex
    }

    private fun createFinalSegments(segmentState: SegmentState, targetSegments: Int): List<WaveformSegment> {
        if (targetSegments <= 0) return emptyList()
        return List(targetSegments) { i ->
            WaveformSegment(segmentState.minValues[i], segmentState.maxValues[i])
        }
    }

    private fun logSegmentPopulationWarnings(
        segments: List<WaveformSegment>,
        segmentState: SegmentState,
        currentOverallSampleIndex: Long,
        targetSegments: Int,
    ) {
        if (targetSegments <= 0 && currentOverallSampleIndex > 0) {
            println("Warning: Target segments is $targetSegments, no segments generated despite processing $currentOverallSampleIndex samples.")
            return
        }
        if (segments.isNotEmpty() && currentOverallSampleIndex > 0) {
            val meaningfulSegments = segmentState.populated.count { it }
            if (meaningfulSegments == 0) { // Check targetSegments > 0 here
                println("Warning: No segments meaningfully populated despite processing $currentOverallSampleIndex samples out of $targetSegments target segments.")
            }
        }
    }

    private fun calculateDurationMillis(totalSamplesProcessed: Long, sampleRate: Int): Int {
        return if (totalSamplesProcessed > 0 && sampleRate > 0) {
            (totalSamplesProcessed * 1000L / sampleRate).toInt()
        } else {
            0
        }
    }
}
