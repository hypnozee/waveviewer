package com.paradoxcat.waveformtest.domain.usecase

import com.paradoxcat.waveformtest.domain.core.Result
import com.paradoxcat.waveformtest.domain.model.WaveformResultData
import com.paradoxcat.waveformtest.domain.parser.WavHeaderParser
import com.paradoxcat.waveformtest.domain.processor.WavStreamProcessor
import com.paradoxcat.waveformtest.domain.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private const val SUPPORTED_SAMPLE_RATE = 44100
private const val SUPPORTED_BIT_DEPTH = 16
private const val SUPPORTED_CHANNELS = 1

/*
 * Creates waveform data from an audio file.
 * 1. Gets the audio file using the [AudioRepository].
 * 2. Reads the WAV file's header with [WavHeaderParser].
 * 3. Checks if the WAV format is supported (sample rate, bit depth, channels).
 * 4. Processes the audio with [WavStreamProcessor] to get waveform parts and length.
 * 5. Gives back the [WaveformResultData] if successful, or an error.
 */
class GetWaveformUseCase(private val audioRepository: AudioRepository) {

    suspend operator fun invoke(
        uriString: String,
        numSegments: Int,
    ): Result<WaveformResultData> {
        return withContext(Dispatchers.IO) {
            try {
                if (numSegments <= 0) { // Added validation for early exit
                    return@withContext Result.Error("Segments number must be greater than 0. Received: $numSegments")
                }
                audioRepository.getAudioFileInputStream(uriString)?.use { inputStream ->
                    // Parse WAV header
                    val formatInfo =
                        WavHeaderParser.parseWavHeaderAndLocateDataChunk(inputStream, uriString)
                            ?: return@withContext Result.Error("Invalid WAV file: Could not parse header or locate data chunk.")

                    // Validate WAV format (specific business rules from ParadoxCat assignment)
                    if (formatInfo.bitDepth != SUPPORTED_BIT_DEPTH) {
                        return@withContext Result.Error("Unsupported WAV: Only $SUPPORTED_BIT_DEPTH-bit files are supported.")
                    }
                    if (formatInfo.channels != SUPPORTED_CHANNELS) {
                        return@withContext Result.Error("Unsupported WAV: Only $SUPPORTED_CHANNELS-channel (mono) files are supported.")
                    }
                    if (formatInfo.sampleRate != SUPPORTED_SAMPLE_RATE) {
                        return@withContext Result.Error("Unsupported WAV: Only ${SUPPORTED_SAMPLE_RATE / 1000.0}kHz sample rate is supported.")
                    }
                    if (formatInfo.dataChunkDeclaredSize <= 0) {
                        return@withContext Result.Error("Invalid WAV file: Data chunk has no size or is invalid.")
                    }
                    WavStreamProcessor.processStream(inputStream, formatInfo, numSegments)
                }
                    ?: return@withContext Result.Error("Could not open input stream for URI via repository: $uriString")
            } catch (e: IOException) {
                Result.Error("Failed to process WAV file: ${e.message}")
            } catch (e: Exception) {
                Result.Error("Failed to process WAV file: ${e.message}")
            }
        }
    }
}
