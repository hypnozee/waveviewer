package com.waveform.data.player

import android.app.Application
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.waveform.domain.player.model.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AudioPlayerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockPlayer: ExoPlayer = mock()
    private val mockApplication: Application = mock()
    private lateinit var audioPlayer: AudioPlayerImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        audioPlayer = AudioPlayerImpl(mockApplication, playerFactory = { mockPlayer })
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        audioPlayer.release()
    }

    private fun loadAndCaptureListener(): Player.Listener {
        val captor = argumentCaptor<Player.Listener>()
        verify(mockPlayer).addListener(captor.capture())
        return captor.firstValue
    }

    @Test
    fun `Initial playback state`() = runTest {
        assertEquals(PlaybackState.default, audioPlayer.playbackState.value)
    }

    @Test
    fun `Load sets loading state and prepares player`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        assertTrue(audioPlayer.playbackState.value.isLoading)
        assertEquals("content://test/audio", audioPlayer.playbackState.value.currentUriString)
        verify(mockPlayer).prepare()
    }

    @Test
    fun `Load transitions to ready on STATE_READY`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        val listener = loadAndCaptureListener()
        whenever(mockPlayer.duration).thenReturn(3000L)
        listener.onPlaybackStateChanged(Player.STATE_READY)

        val state = audioPlayer.playbackState.value
        assertFalse(state.isLoading)
        assertEquals(3000L, state.totalDurationMillis)
        assertNull(state.error)
    }

    @Test
    fun `Load handles unknown duration gracefully`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        val listener = loadAndCaptureListener()
        whenever(mockPlayer.duration).thenReturn(C.TIME_UNSET)
        listener.onPlaybackStateChanged(Player.STATE_READY)

        assertEquals(0L, audioPlayer.playbackState.value.totalDurationMillis)
    }

    @Test
    fun `Load reports player error via listener`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        val listener = loadAndCaptureListener()
        val error = PlaybackException("Codec error", null, PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)
        listener.onPlayerError(error)

        val state = audioPlayer.playbackState.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `Play calls through to ExoPlayer when ready`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        val listener = loadAndCaptureListener()
        whenever(mockPlayer.duration).thenReturn(1000L)
        listener.onPlaybackStateChanged(Player.STATE_READY)

        audioPlayer.play()

        verify(mockPlayer).play()
    }

    @Test
    fun `Play before load reports error`() = runTest {
        audioPlayer.play()

        assertEquals("Player not initialized. Call load() first.", audioPlayer.playbackState.value.error)
        verify(mockPlayer, never()).play()
    }

    @Test
    fun `Pause calls through to ExoPlayer`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        audioPlayer.pause()

        verify(mockPlayer).pause()
    }

    @Test
    fun `SeekTo clamps position and updates state`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        val listener = loadAndCaptureListener()
        whenever(mockPlayer.duration).thenReturn(2000L)
        listener.onPlaybackStateChanged(Player.STATE_READY)

        audioPlayer.seekTo(1000L)

        verify(mockPlayer).seekTo(1000L)
        assertEquals(1000L, audioPlayer.playbackState.value.currentPositionMillis)
    }

    @Test
    fun `SeekTo clamps to maximum duration`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        val listener = loadAndCaptureListener()
        whenever(mockPlayer.duration).thenReturn(2000L)
        listener.onPlaybackStateChanged(Player.STATE_READY)

        audioPlayer.seekTo(9999L)

        verify(mockPlayer).seekTo(2000L)
    }

    @Test
    fun `SeekTo without player does nothing`() = runTest {
        audioPlayer.seekTo(500L)

        verify(mockPlayer, never()).seekTo(any())
        assertEquals(0L, audioPlayer.playbackState.value.currentPositionMillis)
    }

    @Test
    fun `Stop resets state and retains URI`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        audioPlayer.stop()

        verify(mockPlayer).stop()
        verify(mockPlayer).clearMediaItems()
        val state = audioPlayer.playbackState.value
        assertFalse(state.isPlaying)
        assertEquals(0L, state.currentPositionMillis)
        assertEquals("content://test/audio", state.currentUriString)
    }

    @Test
    fun `Release clears all resources and resets to default state`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        audioPlayer.release()
        advanceUntilIdle()

        verify(mockPlayer).release()
        assertEquals(PlaybackState.default, audioPlayer.playbackState.value)
    }

    @Test
    fun `IsPlaying listener updates state`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        val listener = loadAndCaptureListener()
        listener.onIsPlayingChanged(true)
        assertTrue(audioPlayer.playbackState.value.isPlaying)

        listener.onIsPlayingChanged(false)
        assertFalse(audioPlayer.playbackState.value.isPlaying)
    }

    @Test
    fun `Playback completion sets position to total duration`() = runTest {
        audioPlayer.load("content://test/audio")
        advanceUntilIdle()

        val listener = loadAndCaptureListener()
        whenever(mockPlayer.duration).thenReturn(5000L)
        listener.onPlaybackStateChanged(Player.STATE_READY)
        listener.onPlaybackStateChanged(Player.STATE_ENDED)

        val state = audioPlayer.playbackState.value
        assertFalse(state.isPlaying)
        assertEquals(5000L, state.currentPositionMillis)
    }
}
