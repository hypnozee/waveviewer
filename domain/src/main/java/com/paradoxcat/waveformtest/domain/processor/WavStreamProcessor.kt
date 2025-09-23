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
    private const val SEGMENT_RESOLUTION_FRAMES = 256

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

            val bytesPerSample = EXPECTED_BIT_DEPTH / 8
            val bytesPerFrame = bytesPerSample * EXPECTED_CHANNELS

            if (formatInfo.dataChunkDeclaredSize == 0L) {
                return Result.Success(WaveformResultData(emptyList(), 0))
            }
            if (formatInfo.sampleRate <= 0) {
                return Result.Error("Invalid sampleRate: ${formatInfo.sampleRate}")
            }

            val segments = mutableListOf<WaveformSegment>()
            val readBuffer = ByteArray(bytesPerFrame * SEGMENT_RESOLUTION_FRAMES)
            var totalBytesSuccessfullyReadFromStream = 0L

            while (totalBytesSuccessfullyReadFromStream < formatInfo.dataChunkDeclaredSize) {
                val bytesRemainingInChunk =
                    formatInfo.dataChunkDeclaredSize - totalBytesSuccessfullyReadFromStream
                val bytesToReadForThisBlock =
                    min(readBuffer.size.toLong(), bytesRemainingInChunk).toInt()

                if (bytesToReadForThisBlock == 0) break

                val actualBytesReadForBlock =
                    inputStream.read(readBuffer, 0, bytesToReadForThisBlock)

                if (actualBytesReadForBlock == -1) {
                    if (totalBytesSuccessfullyReadFromStream == 0L && bytesRemainingInChunk > 0) {
                        return Result.Error("Reached EOF at the start of the data chunk but data was expected.")
                    }
                    break
                }
                if (actualBytesReadForBlock == 0) break

                totalBytesSuccessfullyReadFromStream += actualBytesReadForBlock

                val framesInThisBlock = actualBytesReadForBlock / bytesPerFrame
                if (framesInThisBlock == 0) continue

                var minBlockAmplitude = Float.MAX_VALUE
                var maxBlockAmplitude = Float.MIN_VALUE
                var framesProcessedInBlock = 0

                for (frameIdx in 0 until framesInThisBlock) {
                    val frameStartOffset = frameIdx * bytesPerFrame
                    val sampleByteBuffer =
                        ByteBuffer.wrap(readBuffer, frameStartOffset, bytesPerSample)
                            .order(ByteOrder.LITTLE_ENDIAN)

                    // Simplified to 16-bit PCM
                    val rawSample = sampleByteBuffer.short.toInt()
                    val normalizedSample = rawSample / 32768.0f

                    minBlockAmplitude = min(minBlockAmplitude, normalizedSample)
                    maxBlockAmplitude = max(maxBlockAmplitude, normalizedSample)
                    framesProcessedInBlock++
                }

                if (framesProcessedInBlock > 0) {
                    segments.add(WaveformSegment(minBlockAmplitude, maxBlockAmplitude))
                }

                if (actualBytesReadForBlock < bytesToReadForThisBlock) {
                    break
                }
            }

            val actualPlayableFrames = totalBytesSuccessfullyReadFromStream / bytesPerFrame
            val durationMillis = if (actualPlayableFrames > 0) {
                (actualPlayableFrames * 1000L / formatInfo.sampleRate).toInt()
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
