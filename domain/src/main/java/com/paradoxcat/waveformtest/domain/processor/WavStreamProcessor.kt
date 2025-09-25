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
    private const val BYTES_PER_FRAME = 2
    private const val TARGET_SEGMENTS = 750
    private const val STREAM_READ_BUFFER_SIZE = 4096
    private const val MAX_16_BIT = 32768.0f

    fun processStream(
        inputStream: InputStream,
        formatInfo: WavFormatInfo,
    ): Result<WaveformResultData> {
        try {
            // Initial Validations
            validateFormat(formatInfo)?.let { return it }

            if (formatInfo.dataChunkDeclaredSize == 0L) {
                return Result.Success(WaveformResultData(emptyList(), 0))
            }

            val totalExpectedSamples =
                formatInfo.dataChunkDeclaredSize / (EXPECTED_BIT_DEPTH / 8 * EXPECTED_CHANNELS)
            if (totalExpectedSamples <= 0) {
                return Result.Success(WaveformResultData(emptyList(), 0))
            }

            val segmentState = initializeSegmentState(totalExpectedSamples)
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
                    if (totalBytesSuccessfullyReadFromStream == 0L) {
                        return Result.Error("Reached EOF at the start of the data chunk when data was expected.")
                    }
                    break
                }
                if (actualBytesReadThisPass == 0) break // Should not happen if bytesToReadForThisPass > 0

                totalBytesSuccessfullyReadFromStream += actualBytesReadThisPass
                currentOverallSampleIndex = processSamplesInCurrentBuffer(
                    readBuffer,
                    actualBytesReadThisPass,
                    segmentState,
                    currentOverallSampleIndex
                )

                if (actualBytesReadThisPass < bytesToReadForThisPass) break // Read less than expected, likely EOF
            }

            val segments = createFinalSegments(segmentState)
            logSegmentPopulationWarnings(segments, segmentState, currentOverallSampleIndex)

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
        if (formatInfo.sampleRate <= 0) { // Though GetWaveformUseCase also checks this, defense in depth.
            return Result.Error("Invalid sampleRate: ${formatInfo.sampleRate}")
        }
        return null // Format is valid
    }

    private fun initializeSegmentState(totalExpectedSamples: Long): SegmentState {
        val samplesPerSegmentValue = totalExpectedSamples.toDouble() / TARGET_SEGMENTS
        return SegmentState(
            minValues = FloatArray(TARGET_SEGMENTS) { Float.POSITIVE_INFINITY },
            maxValues = FloatArray(TARGET_SEGMENTS) { Float.NEGATIVE_INFINITY },
            populated = BooleanArray(TARGET_SEGMENTS) { false },
            samplesPerSegment = samplesPerSegmentValue
        )
    }

    private fun processSamplesInCurrentBuffer(
        buffer: ByteArray,
        bytesRead: Int,
        segmentState: SegmentState,
        startingSampleIndex: Long,
    ): Long {
        var currentOverallSampleIndex = startingSampleIndex
        val samplesInThisBuffer =
            (bytesRead / BYTES_PER_FRAME) // Integer division gives whole samples

        for (i in 0 until samplesInThisBuffer) {
            val sampleOffsetInReadBuffer = i * BYTES_PER_FRAME
            val sampleByteBuffer = ByteBuffer.wrap(
                buffer,
                sampleOffsetInReadBuffer,
                BYTES_PER_FRAME
            )
                .order(ByteOrder.LITTLE_ENDIAN)

            val rawSample = sampleByteBuffer.short.toInt()
            val normalizedSample = rawSample / MAX_16_BIT

            val targetSegmentIndex = if (segmentState.samplesPerSegment > 0) {
                (currentOverallSampleIndex / segmentState.samplesPerSegment).toInt()
            } else {
                // If samplesPerSegment is 0 (e.g. TARGET_SEGMENTS is 0 or very large)
                // or if TARGET_SEGMENTS is 0, map to currentOverallSampleIndex if possible
                currentOverallSampleIndex.toInt().coerceIn(0, TARGET_SEGMENTS - 1)
            }.coerceIn(0, TARGET_SEGMENTS - 1) // Ensure index is always valid

            // Ensure we don't try to access arrays of size 0
            segmentState.minValues[targetSegmentIndex] =
                min(segmentState.minValues[targetSegmentIndex], normalizedSample)
            segmentState.maxValues[targetSegmentIndex] =
                max(segmentState.maxValues[targetSegmentIndex], normalizedSample)
            segmentState.populated[targetSegmentIndex] = true
            currentOverallSampleIndex++
        }
        return currentOverallSampleIndex
    }

    private fun createFinalSegments(segmentState: SegmentState): List<WaveformSegment> {
        return List(TARGET_SEGMENTS) { i ->
            WaveformSegment(segmentState.minValues[i], segmentState.maxValues[i])
        }
    }

    private fun logSegmentPopulationWarnings(
        segments: List<WaveformSegment>,
        segmentState: SegmentState,
        currentOverallSampleIndex: Long,
    ) {
        if (segments.isNotEmpty() && currentOverallSampleIndex > 0) {
            val meaningfulSegments = segmentState.populated.count { it }
            if (meaningfulSegments == 0) {
                println("Warning: No segments meaningfully populated despite processing $currentOverallSampleIndex samples out of $TARGET_SEGMENTS target segments.")
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
