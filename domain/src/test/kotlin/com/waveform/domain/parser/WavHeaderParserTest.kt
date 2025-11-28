package com.waveform.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavHeaderParserTest {

    private fun createValidWavHeader(
        channels: Short = 1,
        sampleRate: Int = 44100,
        bitDepth: Short = 16,
        dataChunkSize: Int = 4096,
    ): ByteArray {
        val byteBuffer = ByteBuffer.allocate(44) // Minimum WAV header size
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        // RIFF
        byteBuffer.put("RIFF".toByteArray())
        byteBuffer.putInt(36 + dataChunkSize) // ChunkSize (36 + SubChunk2Size) 4 bytes is 36 bits
        byteBuffer.put("WAVE".toByteArray())

        // "fmt "
        byteBuffer.put("fmt ".toByteArray())
        byteBuffer.putInt(16) // Subchunk1Size for PCM
        byteBuffer.putShort(1) // AudioFormat (PCM = 1)
        byteBuffer.putShort(channels) // NumChannels
        byteBuffer.putInt(sampleRate) // SampleRate
        byteBuffer.putInt(sampleRate * channels * bitDepth / 8) // ByteRate
        byteBuffer.putShort((channels * bitDepth / 8).toShort()) // BlockAlign
        byteBuffer.putShort(bitDepth) // BitsPerSample

        // "data"
        byteBuffer.put("data".toByteArray())
        byteBuffer.putInt(dataChunkSize) // Subchunk2Size

        return byteBuffer.array()
    }

    @Test
    fun `parseWavHeaderAndLocateDataChunk with valid 16-bit mono WAV returns correct WavFormatInfo`() {
        val sampleRate = 44100
        val dataChunkSize = 4096
        val headerBytes = createValidWavHeader(
            channels = 1,
            sampleRate = sampleRate,
            bitDepth = 16,
            dataChunkSize = dataChunkSize
        )
        val inputStream = ByteArrayInputStream(headerBytes)

        val result = WavHeaderParser.parseWavHeaderAndLocateDataChunk(inputStream, "test.wav")

        assertNotNull(result)
        assertEquals(1, result!!.channels)
        assertEquals(sampleRate, result.sampleRate)
        assertEquals(16, result.bitDepth)
        assertEquals(dataChunkSize.toLong(), result.dataChunkDeclaredSize)
    }

    @Test
    fun `parseWavHeaderAndLocateDataChunk with stereo WAV throws IOException`() {
        val headerBytes = createValidWavHeader(channels = 2) // Stereo
        val inputStream = ByteArrayInputStream(headerBytes)

        val exception = assertThrows(IOException::class.java) {
            WavHeaderParser.parseWavHeaderAndLocateDataChunk(inputStream, "stereo.wav")
        }
        assertEquals(
            "Unsupported WAV format in stereo.wav: Expected 1 channel(s) and 16-bit. Got 2 channel(s) and 16-bit.",
            exception.message
        )
    }

    @Test
    fun `parseWavHeaderAndLocateDataChunk with 8-bit WAV throws IOException`() {
        val headerBytes = createValidWavHeader(bitDepth = 8)
        val inputStream = ByteArrayInputStream(headerBytes)

        val exception = assertThrows(IOException::class.java) {
            WavHeaderParser.parseWavHeaderAndLocateDataChunk(inputStream, "8bit.wav")
        }
        assertEquals(
            "Unsupported WAV format in 8bit.wav: Expected 1 channel(s) and 16-bit. Got 1 channel(s) and 8-bit.",
            exception.message
        )
    }

    @Test
    fun `parseWavHeaderAndLocateDataChunk with non-PCM format throws IOException`() {
        val headerBytes = createValidWavHeader().clone()
        // Modify audio format to something other than 1 (PCM)
        ByteBuffer.wrap(headerBytes, 20, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(2)
        val inputStream = ByteArrayInputStream(headerBytes)

        val exception = assertThrows(IOException::class.java) {
            WavHeaderParser.parseWavHeaderAndLocateDataChunk(inputStream, "non_pcm.wav")
        }
        assertEquals(
            "Unsupported audio format in 'fmt ' chunk (expected PCM=1, got 2) in non_pcm.wav.",
            exception.message
        )
    }

    @Test
    fun `parseWavHeaderAndLocateDataChunk with missing WAVE signature throws IOException`() {
        val riffWaveHeader = ByteBuffer.allocate(12)
        riffWaveHeader.put("RIFF".toByteArray())
        riffWaveHeader.putInt(0)
        riffWaveHeader.put("WAVI".toByteArray()) // Incorrect "WAVE"

        val inputStream2 = ByteArrayInputStream(riffWaveHeader.array())
        val exception2 = assertThrows(IOException::class.java) {
            WavHeaderParser.parseWavHeaderAndLocateDataChunk(inputStream2, "no_wave_signature.wav")
        }
        assertEquals("Invalid RIFF/WAVE signature in no_wave_signature.wav.", exception2.message)
    }
}
