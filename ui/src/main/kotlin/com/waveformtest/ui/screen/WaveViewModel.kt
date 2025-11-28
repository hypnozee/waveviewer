package com.waveformtest.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveformtest.domain.core.Result
import com.waveformtest.domain.model.WaveformResultData
import com.waveformtest.domain.model.WaveformSegment
import com.waveformtest.domain.player.usecase.LoadAudioUseCase
import com.waveformtest.domain.player.usecase.ObservePlaybackStateUseCase
import com.waveformtest.domain.player.usecase.PauseAudioUseCase
import com.waveformtest.domain.player.usecase.PlayAudioUseCase
import com.waveformtest.domain.player.usecase.ReleasePlayerUseCase
import com.waveformtest.domain.player.usecase.SeekAudioUseCase
import com.waveformtest.domain.player.usecase.StopAudioUseCase
import com.waveformtest.domain.usecase.MillisToDigitalClockUseCase
import com.waveformtest.domain.usecase.GetAudioTrackDetailsUseCase
import com.waveformtest.domain.usecase.GetWaveformUseCase
import com.waveformtest.ui.screen.WaveScreenIntent
import com.waveformtest.ui.screen.WaveScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

class WaveViewModel(
    private val getWaveformUseCase: GetWaveformUseCase,
    private val getAudioTrackDetailsUseCase: GetAudioTrackDetailsUseCase,
    private val loadAudioUseCase: LoadAudioUseCase,
    private val playAudioUseCase: PlayAudioUseCase,
    private val pauseAudioUseCase: PauseAudioUseCase,
    private val seekAudioUseCase: SeekAudioUseCase,
    private val stopAudioUseCase: StopAudioUseCase,
    private val observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    private val releasePlayerUseCase: ReleasePlayerUseCase,
    private val millisToDigitalClockUseCase: MillisToDigitalClockUseCase
) : ViewModel() {

    private val _viewState = MutableStateFlow(WaveScreenState())
    val viewStateFlow = _viewState.asStateFlow()

    private data class CacheKey(val uri: String, val numSegments: Int)
    private val waveformCache = mutableMapOf<CacheKey, WaveformResultData>()

    init {
        viewModelScope.launch {
            var previousIsPlaying = false
            observePlaybackStateUseCase().collect { playbackState ->
                _viewState.update { currentState ->
                    val latestErrorMessage = playbackState.error ?: currentState.errorMessage
                    val currentFileUriString = currentState.fileUri?.toString()

                    // Check cache with current segment count
                    val cachedDataForUri = if (currentFileUriString != null) {
                        waveformCache[CacheKey(currentFileUriString, currentState.currentNumSegments)]
                    } else {
                        null
                    }

                    val resolvedTotalDuration =
                        if (playbackState.totalDurationMillis == 0L && cachedDataForUri != null) {
                            cachedDataForUri.durationMillis.toLong()
                        } else {
                            playbackState.totalDurationMillis
                        }

                    var newCurrentPositionMillis = playbackState.currentPositionMillis
                    val newIsPlaying = playbackState.isPlaying

                    // Check for playback completion
                    if (previousIsPlaying && !newIsPlaying && resolvedTotalDuration > 0 &&
                        abs(playbackState.currentPositionMillis - resolvedTotalDuration) < 500
                    ) {
                        newCurrentPositionMillis = 0L
                    }

                    previousIsPlaying = newIsPlaying

                    currentState.copy(
                        isPlayerLoading = playbackState.isLoading,
                        isPlaying = newIsPlaying,
                        currentPositionMillis = newCurrentPositionMillis,
                        totalDurationMillis = resolvedTotalDuration,
                        currentAudioPosition = millisToDigitalClockUseCase(newCurrentPositionMillis),
                        totalAudioDuration = millisToDigitalClockUseCase(resolvedTotalDuration),
                        errorMessage = if (playbackState.error != null) latestErrorMessage else currentState.errorMessage
                    )
                }
            }
        }
    }

    private fun calculateDisplayAmplitudeRange(
        waveformData: List<WaveformSegment>?,
        dynamicNormalizationEnabled: Boolean
    ): Pair<Float, Float> {
        return if (dynamicNormalizationEnabled && waveformData != null && waveformData.isNotEmpty()) {
            val trueMin = waveformData.minOfOrNull { it.min } ?: -1f
            val trueMax = waveformData.maxOfOrNull { it.max } ?: 1f
            trueMin to trueMax
        } else {
            -1f to 1f
        }
    }

    fun processIntent(intent: WaveScreenIntent) {
        when (intent) {
            is WaveScreenIntent.PickFileClicked -> {
                _viewState.update {
                    val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(null, it.dynamicNormalizationEnabled)
                    it.copy(
                        errorMessage = null,
                        waveformData = null,
                        displayMinAmplitude = minAmp,
                        displayMaxAmplitude = maxAmp
                    )
                }
            }

            is WaveScreenIntent.FileSelected -> {
                val selectedUri = intent.uri
                val selectedUriString = selectedUri.toString()
                val segmentsToProcess = _viewState.value.currentNumSegments

                _viewState.update { // Initial update before launching coroutine
                    val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(null, it.dynamicNormalizationEnabled)
                    it.copy(
                        fileUri = selectedUri,
                        fileName = null,
                        isLoadingFile = false,
                        waveformData = null,
                        errorMessage = null,
                        totalDurationMillis = 0L,
                        currentPositionMillis = 0L,
                        currentAudioPosition = millisToDigitalClockUseCase(0L),
                        totalAudioDuration = millisToDigitalClockUseCase(0L),
                        isPlaying = false,
                        isLoadingWaveform = true,
                        isPlayerLoading = true,
                        displayMinAmplitude = minAmp,
                        displayMaxAmplitude = maxAmp
                    )
                }

                viewModelScope.launch {
                    try {
                        stopAudioUseCase()
                        loadAudioUseCase(selectedUriString)

                        getAudioTrackDetailsUseCase(selectedUriString).also { detailsResult ->
                            when (detailsResult) {
                                is Result.Success -> {
                                    _viewState.update { it.copy(fileName = detailsResult.data.fileName) }
                                }
                                is Result.Error -> {
                                    _viewState.update { state ->
                                        val currentError = state.errorMessage
                                        val newError = detailsResult.message
                                        state.copy(
                                            errorMessage = if (currentError != null && !currentError.contains(newError)) "$currentError\n$newError" else newError
                                        )
                                    }
                                }
                            }
                        }

                        // Check cache first!
                        val cacheKey = CacheKey(selectedUriString, segmentsToProcess)
                        val cachedData = waveformCache[cacheKey]

                        if (cachedData != null) {
                            // Use cached data immediately
                            _viewState.update { state ->
                                val newTotalDuration = if (state.totalDurationMillis == 0L) {
                                    cachedData.durationMillis.toLong()
                                } else {
                                    state.totalDurationMillis
                                }
                                val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(
                                    cachedData.waveformSegments,
                                    state.dynamicNormalizationEnabled
                                )
                                state.copy(
                                    waveformData = cachedData.waveformSegments,
                                    totalDurationMillis = newTotalDuration,
                                    totalAudioDuration = millisToDigitalClockUseCase(newTotalDuration),
                                    displayMinAmplitude = minAmp,
                                    displayMaxAmplitude = maxAmp,
                                    isLoadingWaveform = false
                                )
                            }
                        } else {
                            // Not in cache, process it
                            when (val waveformResult = getWaveformUseCase(selectedUriString, segmentsToProcess)) {
                                is Result.Success -> {
                                    waveformCache[cacheKey] = waveformResult.data
                                    _viewState.update { state ->
                                        val newTotalDuration = if (state.totalDurationMillis == 0L) {
                                            waveformResult.data.durationMillis.toLong()
                                        } else {
                                            state.totalDurationMillis
                                        }
                                        val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(
                                            waveformResult.data.waveformSegments,
                                            state.dynamicNormalizationEnabled
                                        )
                                        state.copy(
                                            waveformData = waveformResult.data.waveformSegments,
                                            totalDurationMillis = newTotalDuration,
                                            totalAudioDuration = millisToDigitalClockUseCase(newTotalDuration),
                                            displayMinAmplitude = minAmp,
                                            displayMaxAmplitude = maxAmp,
                                            isLoadingWaveform = false
                                        )
                                    }
                                }
                                is Result.Error -> {
                                    _viewState.update { state ->
                                        val currentError = state.errorMessage
                                        val newError = waveformResult.message
                                        state.copy(
                                            errorMessage = if (currentError != null && !currentError.contains(newError)) "$currentError\n$newError" else newError,
                                            isLoadingWaveform = false
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        _viewState.update { state ->
                            val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(state.waveformData, state.dynamicNormalizationEnabled)
                            state.copy(
                                errorMessage = "An unexpected error occurred: ${e.message}",
                                isPlayerLoading = false,
                                isLoadingWaveform = false,
                                displayMinAmplitude = minAmp,
                                displayMaxAmplitude = maxAmp
                            )
                        }
                    }
                }
            }

            is WaveScreenIntent.NumSegmentsChanged -> {
                val newNumSegments = intent.newNumber.coerceIn(MIN_NUM_SEGMENTS, MAX_NUM_SEGMENTS)
                val currentFileUri = _viewState.value.fileUri

                _viewState.update {
                    val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(if (currentFileUri != null) null else it.waveformData, it.dynamicNormalizationEnabled)
                    it.copy(
                        currentNumSegments = newNumSegments,
                        isLoadingWaveform = if (currentFileUri != null) true else it.isLoadingWaveform,
                        waveformData = if (currentFileUri != null) null else it.waveformData,
                        displayMinAmplitude = minAmp,
                        displayMaxAmplitude = maxAmp
                    )
                }

                if (currentFileUri != null) {
                    val currentFileUriString = currentFileUri.toString()
                    viewModelScope.launch {
                        try {
                            // Check cache first!
                            val cacheKey = CacheKey(currentFileUriString, newNumSegments)
                            val cachedData = waveformCache[cacheKey]

                            if (cachedData != null) {
                                // Use cached data immediately
                                _viewState.update { state ->
                                    val newTotalDuration = if (state.totalDurationMillis == 0L) {
                                        cachedData.durationMillis.toLong()
                                    } else {
                                        state.totalDurationMillis
                                    }
                                    val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(
                                        cachedData.waveformSegments,
                                        state.dynamicNormalizationEnabled
                                    )
                                    state.copy(
                                        waveformData = cachedData.waveformSegments,
                                        totalDurationMillis = newTotalDuration,
                                        totalAudioDuration = millisToDigitalClockUseCase(newTotalDuration),
                                        displayMinAmplitude = minAmp,
                                        displayMaxAmplitude = maxAmp,
                                        isLoadingWaveform = false
                                    )
                                }
                            } else {
                                // Not in cache, process it
                                when (val waveformResult = getWaveformUseCase(currentFileUriString, newNumSegments)) {
                                    is Result.Success -> {
                                        waveformCache[cacheKey] = waveformResult.data
                                        _viewState.update { state ->
                                            val newTotalDuration = if (state.totalDurationMillis == 0L || waveformCache[cacheKey]?.durationMillis?.toLong() != state.totalDurationMillis) {
                                                waveformResult.data.durationMillis.toLong()
                                            } else {
                                                state.totalDurationMillis
                                            }
                                            val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(
                                                waveformResult.data.waveformSegments,
                                                state.dynamicNormalizationEnabled
                                            )
                                            state.copy(
                                                waveformData = waveformResult.data.waveformSegments,
                                                totalDurationMillis = newTotalDuration,
                                                totalAudioDuration = millisToDigitalClockUseCase(newTotalDuration),
                                                displayMinAmplitude = minAmp,
                                                displayMaxAmplitude = maxAmp,
                                                isLoadingWaveform = false
                                            )
                                        }
                                    }
                                    is Result.Error -> {
                                        _viewState.update { state ->
                                            val currentError = state.errorMessage
                                            val newError = waveformResult.message
                                            state.copy(
                                                errorMessage = if (currentError != null && !currentError.contains(newError)) "$currentError\n$newError" else newError,
                                                isLoadingWaveform = false
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            _viewState.update { state ->
                                val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(state.waveformData, state.dynamicNormalizationEnabled)
                                state.copy(
                                    errorMessage = "Error re-processing waveform: ${e.message}",
                                    isLoadingWaveform = false,
                                    displayMinAmplitude = minAmp,
                                    displayMaxAmplitude = maxAmp
                                )
                            }
                        }
                    }
                }
            }

            is WaveScreenIntent.ToggleDynamicNormalization -> {
                _viewState.update { currentState ->
                    val newNormalizationState = !currentState.dynamicNormalizationEnabled
                    val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(currentState.waveformData, newNormalizationState)
                    currentState.copy(
                        dynamicNormalizationEnabled = newNormalizationState,
                        displayMinAmplitude = minAmp,
                        displayMaxAmplitude = maxAmp
                    )
                }
            }

            is WaveScreenIntent.PlayPauseClicked -> {
                if (_viewState.value.isPlaying) {
                    pauseAudioUseCase()
                } else {
                    if (_viewState.value.fileUri != null) {
                        if (!_viewState.value.isPlayerLoading) {
                            if (_viewState.value.totalDurationMillis > 0 && abs(_viewState.value.currentPositionMillis - _viewState.value.totalDurationMillis) < 500) {
                                seekAudioUseCase(0L)
                            }
                            playAudioUseCase()
                        } else {
                            _viewState.update { it.copy(errorMessage = "Player is still loading the audio.") }
                        }
                    } else {
                        _viewState.update { it.copy(errorMessage = "No audio file loaded.") }
                    }
                }
            }

            is WaveScreenIntent.SeekTo -> {
                val totalDuration = _viewState.value.totalDurationMillis
                if (totalDuration > 0 && !_viewState.value.isPlayerLoading) {
                    val newPosition = (totalDuration * intent.positionFraction).toLong()
                    seekAudioUseCase(newPosition)
                }
            }

            is WaveScreenIntent.ClearErrorMessage -> {
                _viewState.update { it.copy(errorMessage = null) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayerUseCase()
        waveformCache.clear()
    }
}