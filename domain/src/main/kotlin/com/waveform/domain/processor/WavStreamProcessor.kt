package com.waveform.domain.processor

import com.waveform.domain.core.Result
import com.waveform.domain.model.WavFormatInfo
import com.waveform.domain.model.WaveformResultData
import com.waveform.domain.model.WaveformSegment
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class WavStreamProcessor {
    companion object {
        private const val EXPECTED_CHANNELS = 1
        private const val EXPECTED_BIT_DEPTH = 16
        private const val STREAM_READ_BUFFER_SIZE = 4096
        private const val MAX_16_BIT = 32768.0f
        private const val BYTES_PER_FRAME_MONO_16_BIT = 2 /**  EXPECTED_BIT_DEPTH/8 * EXPECTED_CHANNELS  **/
    }

    // Processing-internal state; not part of the public domain model surface (#11)
    private data class SegmentState(
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

    fun processStream(
        inputStream: InputStream,
        formatInfo: WavFormatInfo,
        numSegments: Int,
    ): Result<WaveformResultData> {
        try {
            validateFormat(formatInfo)?.let { return it }

            if (numSegments <= 0) {
                return Result.Error("Segments number must be greater than 0. Received: $numSegments")
            }

            if (formatInfo.dataChunkDeclaredSize == 0L) {
                return Result.Success(WaveformResultData(emptyList(), 0))
            }

            val totalExpectedSamples =
                formatInfo.dataChunkDeclaredSize / BYTES_PER_FRAME_MONO_16_BIT
            if (totalExpectedSamples <= 0) {
                return Result.Success(WaveformResultData(emptyList(), 0))
            }

            val segmentState = initializeSegmentState(totalExpectedSamples, numSegments)
            val readBuffer = ByteArray(STREAM_READ_BUFFER_SIZE)
            var totalBytesSuccessfullyReadFromStream = 0L
            var sampleIndex = 0L

            while (totalBytesSuccessfullyReadFromStream < formatInfo.dataChunkDeclaredSize) {
                val bytesToReadForThisPass = min(
                    readBuffer.size.toLong(),
                    formatInfo.dataChunkDeclaredSize - totalBytesSuccessfullyReadFromStream
                ).toInt()
                if (bytesToReadForThisPass == 0) break

                val actualBytesReadThisPass =
                    inputStream.read(readBuffer, 0, bytesToReadForThisPass)
                if (actualBytesReadThisPass == -1) {
                    if (totalBytesSuccessfullyReadFromStream == 0L) {
                        return Result.Error("Reached EOF at the start of the data chunk when data was expected.")
                    }
                    break
                }
                if (actualBytesReadThisPass == 0) break

                totalBytesSuccessfullyReadFromStream += actualBytesReadThisPass
                sampleIndex = processSamplesInCurrentBuffer(
                    readBuffer,
                    actualBytesReadThisPass,
                    segmentState,
                    sampleIndex,
                    numSegments,
                )

                if (actualBytesReadThisPass < bytesToReadForThisPass) break
            }

            val segments = createFinalSegments(segmentState, numSegments)
            logSegmentPopulationWarnings(segments, segmentState, sampleIndex, numSegments)

            val durationMillis = calculateDurationMillis(sampleIndex, formatInfo.sampleRate)
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

    private fun initializeSegmentState(totalExpectedSamples: Long, numSegments: Int): SegmentState {
        val samplesPerSegmentValue = if (numSegments > 0) {
            totalExpectedSamples.toDouble() / numSegments
        } else {
            0.0
        }
        return SegmentState(
            minValues = FloatArray(numSegments) { MAX_16_BIT },
            maxValues = FloatArray(numSegments) { -MAX_16_BIT },
            populated = BooleanArray(numSegments) { false },
            samplesPerSegment = samplesPerSegmentValue
        )
    }

    private fun processSamplesInCurrentBuffer(
        buffer: ByteArray,
        bytesRead: Int,
        segmentState: SegmentState,
        startingSampleIndex: Long,
        numSegments: Int,
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

            if (numSegments > 0) {
                val segmentsIndex = if (segmentState.samplesPerSegment > 0) {
                    (currentOverallSampleIndex / segmentState.samplesPerSegment).toInt()
                } else {
                    currentOverallSampleIndex.toInt().coerceIn(0, numSegments - 1)
                }.coerceIn(0, numSegments - 1)

                segmentState.minValues[segmentsIndex] =
                    min(segmentState.minValues[segmentsIndex], normalizedSample)
                segmentState.maxValues[segmentsIndex] =
                    max(segmentState.maxValues[segmentsIndex], normalizedSample)
                segmentState.populated[segmentsIndex] = true
            }
            currentOverallSampleIndex++
        }
        return currentOverallSampleIndex
    }

    private fun createFinalSegments(segmentState: SegmentState, numSegments: Int): List<WaveformSegment> {
        if (numSegments <= 0) return emptyList()
        return List(numSegments) { i ->
            WaveformSegment(segmentState.minValues[i], segmentState.maxValues[i])
        }
    }

    private fun logSegmentPopulationWarnings(
        segments: List<WaveformSegment>,
        segmentState: SegmentState,
        sampleIndex: Long,
        numSegments: Int,
    ) {
        if (numSegments <= 0 && sampleIndex > 0) {
            println("Warning: Segments number is $numSegments, no segments generated despite processing $sampleIndex samples.")
            return
        }
        if (segments.isNotEmpty() && sampleIndex > 0) {
            val meaningfulSegments = segmentState.populated.count { it }
            if (meaningfulSegments == 0) {
                println("Warning: No segments meaningfully populated despite processing $sampleIndex samples out of $numSegments Segments number.")
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
