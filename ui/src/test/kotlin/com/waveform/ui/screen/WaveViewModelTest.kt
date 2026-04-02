package com.waveform.ui.screen

import android.net.Uri
import com.waveform.domain.core.Result
import com.waveform.domain.model.AudioTrackDetails
import com.waveform.domain.model.WaveformResultData
import com.waveform.domain.model.WaveformSegment
import com.waveform.domain.player.model.PlaybackState
import com.waveform.domain.player.usecase.LoadAudioUseCase
import com.waveform.domain.player.usecase.ObservePlaybackStateUseCase
import com.waveform.domain.player.usecase.PauseAudioUseCase
import com.waveform.domain.player.usecase.PlayAudioUseCase
import com.waveform.domain.player.usecase.ReleasePlayerUseCase
import com.waveform.domain.player.usecase.SeekAudioUseCase
import com.waveform.domain.player.usecase.StopAudioUseCase
import com.waveform.domain.usecase.GetAudioTrackDetailsUseCase
import com.waveform.domain.usecase.GetWaveformUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WaveViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: WaveViewModel

    private val getWaveformUseCase: GetWaveformUseCase = mock()
    private val getAudioTrackDetailsUseCase: GetAudioTrackDetailsUseCase = mock()
    private val loadAudioUseCase: LoadAudioUseCase = mock()
    private val playAudioUseCase: PlayAudioUseCase = mock()
    private val pauseAudioUseCase: PauseAudioUseCase = mock()
    private val seekAudioUseCase: SeekAudioUseCase = mock()
    private val stopAudioUseCase: StopAudioUseCase = mock()
    private val observePlaybackStateUseCase: ObservePlaybackStateUseCase = mock()
    private val releasePlayerUseCase: ReleasePlayerUseCase = mock()

    private val playbackStateFlow = MutableStateFlow(PlaybackState.default)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(observePlaybackStateUseCase()).thenReturn(playbackStateFlow.asStateFlow())

        viewModel = WaveViewModel(
            getWaveformUseCase,
            getAudioTrackDetailsUseCase,
            loadAudioUseCase,
            playAudioUseCase,
            pauseAudioUseCase,
            seekAudioUseCase,
            stopAudioUseCase,
            observePlaybackStateUseCase,
            releasePlayerUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `PickFileClicked  Reset state and clearing`() = runTest {
        viewModel.processIntent(WaveScreenIntent.PickFileClicked)

        val state = viewModel.viewStateFlow.value
        assertNull(state.errorMessage)
        assertNull(state.waveformData)
        assertEquals(-1f, state.displayMinAmplitude, 0.01f)
        assertEquals(1f, state.displayMaxAmplitude, 0.01f)
    }

    @Test
    fun `FileSelected  Initial state reset`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("content://test")

        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("test.wav", 1000L, 44100))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(emptyList(), 1000))
        )

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))

        val state = viewModel.viewStateFlow.value
        assertEquals(uri, state.fileUri)
        assertNull(state.fileName)
        assertNull(state.waveformData)
        assertTrue(state.isLoadingWaveform)
        assertTrue(state.isPlayerLoading)
        assertEquals(0L, state.currentPositionMillis)
        assertEquals(0L, state.totalDurationMillis)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `FileSelected  Successful audio details load`() = runTest {
        val uri = mock<Uri>()
        val uriString = "content://test"
        whenever(uri.toString()).thenReturn(uriString)

        val details = AudioTrackDetails("MySong.wav", 5000L, 44100)
        whenever(loadAudioUseCase(uriString)).thenReturn(Unit)
        whenever(getAudioTrackDetailsUseCase(uriString)).thenReturn(Result.Success(details))
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(emptyList(), 5000))
        )

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        assertEquals("MySong.wav", viewModel.viewStateFlow.value.fileName)
    }

    @Test
    fun `FileSelected  Failed audio details load`() = runTest {
        val uri = mock<Uri>()
        val uriString = "content://test"
        whenever(uri.toString()).thenReturn(uriString)

        whenever(loadAudioUseCase(uriString)).thenReturn(Unit)
        whenever(getAudioTrackDetailsUseCase(uriString)).thenReturn(Result.Error("Failed to read details"))
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(emptyList(), 1000))
        )

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        // Details error is non-fatal; waveform still succeeds so the error is preserved
        assertEquals("Failed to read details", viewModel.viewStateFlow.value.errorMessage)
    }

    @Test
    fun `FileSelected  Waveform cache hit`() = runTest {
        val uri = mock<Uri>()
        val uriString = "content://test"
        whenever(uri.toString()).thenReturn(uriString)

        val segments = listOf(WaveformSegment(0.0f, 1.0f))
        val waveformResult = WaveformResultData(segments, 2000)

        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("test", 0, 0))
        )
        whenever(getWaveformUseCase(uriString, DEFAULT_SEGMENTS_NUMBER)).thenReturn(
            Result.Success(waveformResult)
        )

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        org.mockito.kotlin.reset(getWaveformUseCase)

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        verify(getWaveformUseCase, never()).invoke(any(), any())
        val state = viewModel.viewStateFlow.value
        assertEquals(segments, state.waveformData)
        assertFalse(state.isLoadingWaveform)
    }

    @Test
    fun `FileSelected  Waveform cache miss processing success`() = runTest {
        val uri = mock<Uri>()
        val uriString = "content://test"
        whenever(uri.toString()).thenReturn(uriString)

        val segments = listOf(WaveformSegment(-0.5f, 0.5f))
        val result = WaveformResultData(segments, 3000)

        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("test", 0, 0))
        )
        whenever(getWaveformUseCase(uriString, DEFAULT_SEGMENTS_NUMBER)).thenReturn(
            Result.Success(result)
        )

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        verify(getWaveformUseCase).invoke(uriString, DEFAULT_SEGMENTS_NUMBER)
        val state = viewModel.viewStateFlow.value
        assertEquals(segments, state.waveformData)
        assertEquals(3000L, state.totalDurationMillis)
        assertFalse(state.isLoadingWaveform)
    }

    @Test
    fun `FileSelected  Waveform processing failure`() = runTest {
        val uri = mock<Uri>()
        val uriString = "content://test"
        whenever(uri.toString()).thenReturn(uriString)

        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("test", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(Result.Error("Processing failed"))

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        val state = viewModel.viewStateFlow.value
        assertNull(state.waveformData)
        assertEquals("Processing failed", state.errorMessage)
        assertFalse(state.isLoadingWaveform)
    }

    @Test
    fun `FileSelected  Exception handling during launch`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("content://error")

        whenever(stopAudioUseCase()).doThrow(RuntimeException("Unexpected crash"))

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        val state = viewModel.viewStateFlow.value
        assertTrue(state.errorMessage?.contains("Unexpected crash") == true)
        assertFalse(state.isLoadingWaveform)
        assertFalse(state.isPlayerLoading)
    }

    @Test
    fun `NumSegmentsChanged  Range coercion validation`() = runTest {
        viewModel.processIntent(WaveScreenIntent.NumSegmentsChanged(10))
        assertEquals(MIN_NUM_SEGMENTS, viewModel.viewStateFlow.value.currentNumSegments)

        viewModel.processIntent(WaveScreenIntent.NumSegmentsChanged(5000))
        assertEquals(MAX_NUM_SEGMENTS, viewModel.viewStateFlow.value.currentNumSegments)
    }

    @Test
    fun `NumSegmentsChanged  No file loaded`() = runTest {
        viewModel.processIntent(WaveScreenIntent.NumSegmentsChanged(200))

        assertEquals(200, viewModel.viewStateFlow.value.currentNumSegments)
        verify(getWaveformUseCase, never()).invoke(any(), any())
    }

    @Test
    fun `NumSegmentsChanged  File loaded cache hit`() = runTest {
        val uri = mock<Uri>()
        val uriString = "content://test"
        whenever(uri.toString()).thenReturn(uriString)

        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(uriString, 100)).thenReturn(
            Result.Success(WaveformResultData(listOf(), 100))
        )

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        viewModel.processIntent(WaveScreenIntent.NumSegmentsChanged(100))
        advanceUntilIdle()

        org.mockito.kotlin.reset(getWaveformUseCase)

        viewModel.processIntent(WaveScreenIntent.NumSegmentsChanged(100))
        advanceUntilIdle()

        verify(getWaveformUseCase, never()).invoke(any(), any())
    }

    @Test
    fun `NumSegmentsChanged  File loaded cache miss`() = runTest {
        val uri = mock<Uri>()
        val uriString = "content://test"
        whenever(uri.toString()).thenReturn(uriString)

        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(listOf(), 100))
        )

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        val newSegments = 600
        viewModel.processIntent(WaveScreenIntent.NumSegmentsChanged(newSegments))
        advanceUntilIdle()

        verify(getWaveformUseCase).invoke(uriString, newSegments)
        assertFalse(viewModel.viewStateFlow.value.isLoadingWaveform)
    }

    @Test
    fun `NumSegmentsChanged  Processing error`() = runTest {
        val uri = mock<Uri>()
        val uriString = "content://test"
        whenever(uri.toString()).thenReturn(uriString)

        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(listOf(), 100))
        )
        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        whenever(getWaveformUseCase(any(), any())).thenReturn(Result.Error("Resize failed"))

        viewModel.processIntent(WaveScreenIntent.NumSegmentsChanged(800))
        advanceUntilIdle()

        assertTrue(viewModel.viewStateFlow.value.errorMessage?.contains("Resize failed") == true)
        assertFalse(viewModel.viewStateFlow.value.isLoadingWaveform)
    }

    @Test
    fun `ToggleDynamicNormalization  Amplitude recalculation`() = runTest {
        val segments = listOf(WaveformSegment(-0.2f, 0.2f), WaveformSegment(-0.8f, 0.8f))
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("content://test")
        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(segments, 100))
        )

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        assertFalse(viewModel.viewStateFlow.value.dynamicNormalizationEnabled)
        assertEquals(-1f, viewModel.viewStateFlow.value.displayMinAmplitude, 0.01f)
        assertEquals(1f, viewModel.viewStateFlow.value.displayMaxAmplitude, 0.01f)

        viewModel.processIntent(WaveScreenIntent.ToggleDynamicNormalization)

        assertTrue(viewModel.viewStateFlow.value.dynamicNormalizationEnabled)
        assertEquals(-0.8f, viewModel.viewStateFlow.value.displayMinAmplitude, 0.01f)
        assertEquals(0.8f, viewModel.viewStateFlow.value.displayMaxAmplitude, 0.01f)
    }

    @Test
    fun `PlayPauseClicked  Pause when playing`() = runTest {
        playbackStateFlow.value = PlaybackState(isPlaying = true)
        advanceUntilIdle()

        viewModel.processIntent(WaveScreenIntent.PlayPauseClicked)
        verify(pauseAudioUseCase).invoke()
    }

    @Test
    fun `PlayPauseClicked  Play when paused`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("c")
        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(listOf(), 100))
        )
        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        playbackStateFlow.value = PlaybackState(isPlaying = false, isLoading = false)
        advanceUntilIdle()

        viewModel.processIntent(WaveScreenIntent.PlayPauseClicked)
        verify(playAudioUseCase).invoke()
    }

    @Test
    fun `PlayPauseClicked  Restart from end`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("c")
        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(listOf(), 1000))
        )
        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        playbackStateFlow.value = PlaybackState(
            isPlaying = false,
            isLoading = false,
            totalDurationMillis = 1000,
            currentPositionMillis = 900
        )
        advanceUntilIdle()

        viewModel.processIntent(WaveScreenIntent.PlayPauseClicked)

        val captor = argumentCaptor<Long>()
        verify(seekAudioUseCase).invoke(captor.capture())
        assertEquals(0L, captor.firstValue)
        verify(playAudioUseCase).invoke()
    }

    @Test
    fun `PlayPauseClicked  Player loading guard`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("c")
        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(listOf(), 100))
        )
        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        playbackStateFlow.value = PlaybackState(isLoading = true)
        advanceUntilIdle()

        viewModel.processIntent(WaveScreenIntent.PlayPauseClicked)
        verify(playAudioUseCase, never()).invoke()
        assertEquals(
            "Player is still loading the audio.",
            viewModel.viewStateFlow.value.errorMessage
        )
    }

    @Test
    fun `PlayPauseClicked  No file loaded guard`() = runTest {
        viewModel.processIntent(WaveScreenIntent.PlayPauseClicked)
        verify(playAudioUseCase, never()).invoke()
        assertEquals("No audio file loaded.", viewModel.viewStateFlow.value.errorMessage)
    }

    @Test
    fun `SeekTo  Normal seek`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("c")
        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(listOf(), 2000))
        )
        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        playbackStateFlow.value = PlaybackState(totalDurationMillis = 2000, isLoading = false)
        advanceUntilIdle()

        viewModel.processIntent(WaveScreenIntent.SeekTo(0.5f))

        verify(seekAudioUseCase).invoke(1000L)
    }

    @Test
    fun `SeekTo  Loading guard`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("c")
        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(listOf(), 2000))
        )
        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        playbackStateFlow.value = PlaybackState(isLoading = true, totalDurationMillis = 2000)
        advanceUntilIdle()

        viewModel.processIntent(WaveScreenIntent.SeekTo(0.5f))
        verify(seekAudioUseCase, never()).invoke(any())
    }

    @Test
    fun `SeekTo  Zero duration guard`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("c")
        whenever(getAudioTrackDetailsUseCase(any())).thenReturn(
            Result.Success(AudioTrackDetails("t", 0, 0))
        )
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(listOf(), 0))
        )
        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        viewModel.processIntent(WaveScreenIntent.SeekTo(0.5f))
        verify(seekAudioUseCase, never()).invoke(any())
    }

    @Test
    fun `ClearErrorMessage  Reset`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("c")
        whenever(loadAudioUseCase("c")).thenReturn(Unit)
        whenever(getAudioTrackDetailsUseCase("c")).thenReturn(Result.Error("Some error"))
        whenever(getWaveformUseCase(any(), any())).thenReturn(
            Result.Success(WaveformResultData(emptyList(), 0))
        )
        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        assertEquals("Some error", viewModel.viewStateFlow.value.errorMessage)

        viewModel.processIntent(WaveScreenIntent.ClearErrorMessage)
        assertNull(viewModel.viewStateFlow.value.errorMessage)
    }

    @Test
    fun `Combined  Last error wins when multiple errors occur`() = runTest {
        val uri = mock<Uri>()
        whenever(uri.toString()).thenReturn("c")
        whenever(loadAudioUseCase("c")).thenReturn(Unit)
        whenever(getAudioTrackDetailsUseCase("c")).thenReturn(Result.Error("Error 1"))
        whenever(getWaveformUseCase(any(), any())).thenReturn(Result.Error("Error 2"))

        viewModel.processIntent(WaveScreenIntent.FileSelected(uri))
        advanceUntilIdle()

        assertEquals("Error 2", viewModel.viewStateFlow.value.errorMessage)
    }

    @Test
    fun `Combined  Amplitude Calculation Logic`() = runTest {
        viewModel.processIntent(WaveScreenIntent.ToggleDynamicNormalization)
        assertTrue(viewModel.viewStateFlow.value.dynamicNormalizationEnabled)
        assertEquals(-1f, viewModel.viewStateFlow.value.displayMinAmplitude, 0.01f)
        assertEquals(1f, viewModel.viewStateFlow.value.displayMaxAmplitude, 0.01f)
    }
}
