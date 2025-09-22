package com.paradoxcat.waveformtest.domain.parser

import com.paradoxcat.waveformtest.domain.model.WavFormatInfo
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A helper for reading the header of WAV audio files.
 * It looks for important parts like "RIFF", "WAVE", "fmt ", and "data"
 * Interesting theory behind lossless audio decoding.
 */
object WavHeaderParser {

    // We care to find out about:
    //    val channels: Int,
    //    val sampleRate: Int,
    //    val bitDepth: Int,
    // +  dataChunkDeclaredSize,
    fun parseWavHeaderAndLocateDataChunk(
        inputStream: InputStream,
        uriString: String,
    ): WavFormatInfo? {
        try {
            validateRiffWaveHeader(inputStream, uriString)
            var fmtInfo: Triple<Int, Int, Int>? = null

            while (true) {
                val (chunkId, chunkSize) = readChunkHeader(inputStream, uriString)

                when (chunkId) {
                    "fmt " -> {
                        fmtInfo = parseFmtChunk(inputStream, chunkSize, uriString)
                    }

                    "data" -> {
                        if (fmtInfo == null) {
                            throw IOException("'data' chunk found before 'fmt ' in $uriString.")
                        }
                        val (channels, sampleRate, bitDepth) = fmtInfo
                        return WavFormatInfo(channels, sampleRate, bitDepth, chunkSize)
                    }

                    else -> {
                        skipUnknownChunk(inputStream, chunkId, chunkSize, uriString)
                    }
                }
            }
        } catch (e: IOException) {
            println("WavHeaderParser: IOException parsing WAV header for $uriString: ${e.message}")
            throw e
        } catch (e: Exception) {
            println("WavHeaderParser: Unexpected exception parsing WAV header for $uriString: ${e.message}")
            return null
        }
    }

    // Header is 3 slots x 4 bytes
    // first 4 bytes RIFF
    // last 4 bytes WAV
    private fun validateRiffWaveHeader(inputStream: InputStream, uriString: String) {
        val header = ByteArray(12)
        if (inputStream.read(header) != 12) {
            throw IOException("Invalid or truncated RIFF header in $uriString.")
        }
        if (String(header, 0, 4) != "RIFF" || String(header, 8, 4) != "WAVE") {
            throw IOException("Invalid RIFF/WAVE signature in $uriString.")
        }
    }

    private fun readChunkHeader(inputStream: InputStream, uriString: String): Pair<String, Long> {
        val idBytes = ByteArray(4)
        if (inputStream.read(idBytes) != 4) {
            throw IOException("Unexpected EOF while reading chunk ID in $uriString.")
        }
        val chunkId = String(idBytes)

        val sizeBytes = ByteArray(4)
        if (inputStream.read(sizeBytes) != 4) {
            throw IOException("Unexpected EOF while reading chunk size for '$chunkId' in $uriString.")
        }
        val chunkSize = ByteBuffer.wrap(sizeBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
            .toLong()

        if (chunkSize < 0) {
            throw IOException("Chunk '$chunkId' has invalid size $chunkSize in $uriString.")
        }
        return chunkId to chunkSize
    }

    private fun parseFmtChunk(
        inputStream: InputStream,
        size: Long,
        uriString: String,
    ): Triple<Int, Int, Int> {
        if (size < 16) {
            throw IOException("'fmt ' chunk too small ($size bytes) in $uriString.")
        }

        val fmtChunkBytes = ByteArray(16)
        if (inputStream.read(fmtChunkBytes) != 16) {
            throw IOException("Failed to read 'fmt ' chunk content (first 16 bytes) in $uriString.")
        }

        if (size > 16) {
            val remainingToSkip = size - 16
            if (inputStream.skip(remainingToSkip) != remainingToSkip) {
                throw IOException("Failed to skip remaining $remainingToSkip bytes in 'fmt ' chunk in $uriString.")
            }
        }

        val channels =
            ByteBuffer.wrap(fmtChunkBytes, 2, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        val sampleRate = ByteBuffer.wrap(fmtChunkBytes, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val bitDepth =
            ByteBuffer.wrap(fmtChunkBytes, 14, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

        return Triple(channels, sampleRate, bitDepth)
    }

    // We don't care about other info other than fmt, data.
    private fun skipUnknownChunk(
        inputStream: InputStream,
        chunkId: String,
        size: Long,
        uriString: String,
    ) {
        if (size <= 0) return

        var remaining = size
        while (remaining > 0) {
            val skipped = inputStream.skip(remaining)
            if (skipped <= 0) {
                val oneByte = inputStream.read()
                if (oneByte == -1) {
                    throw IOException("Unexpected EOF while skipping chunk '$chunkId' ($size bytes). Intended to skip $remaining more bytes in $uriString.")
                }
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }
}
