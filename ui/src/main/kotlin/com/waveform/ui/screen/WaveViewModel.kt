package com.waveform.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveform.domain.core.Result
import com.waveform.domain.model.WaveformResultData
import com.waveform.domain.model.WaveformSegment
import com.waveform.domain.player.usecase.LoadAudioUseCase
import com.waveform.domain.player.usecase.ObservePlaybackStateUseCase
import com.waveform.domain.player.usecase.PauseAudioUseCase
import com.waveform.domain.player.usecase.PlayAudioUseCase
import com.waveform.domain.player.usecase.ReleasePlayerUseCase
import com.waveform.domain.player.usecase.SeekAudioUseCase
import com.waveform.domain.player.usecase.StopAudioUseCase
import com.waveform.domain.usecase.GetAudioTrackDetailsUseCase
import com.waveform.domain.usecase.GetWaveformUseCase
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
) : ViewModel() {

    private val _viewState = MutableStateFlow(WaveScreenState())
    val viewStateFlow = _viewState.asStateFlow()

    private data class CacheKey(val uri: String, val numSegments: Int)


    // LRU cache capped at 10 entries (#3)
    private val waveformCache: MutableMap<CacheKey, WaveformResultData> =
        object : LinkedHashMap<CacheKey, WaveformResultData>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, WaveformResultData>) =
                size > 10
        }

    init {
        viewModelScope.launch {
            var previousIsPlaying = false
            observePlaybackStateUseCase().collect { playbackState ->
                _viewState.update { currentState ->
                    val currentFileUriString = currentState.fileUri?.toString()

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
                        errorMessage = playbackState.error ?: currentState.errorMessage,
                    )
                }
            }
        }
    }

    private fun calculateDisplayAmplitudeRange(
        waveformData: List<WaveformSegment>?,
        dynamicNormalizationEnabled: Boolean,
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

                _viewState.update {
                    val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(null, it.dynamicNormalizationEnabled)
                    it.copy(
                        fileUri = selectedUri,
                        fileName = null,
                        isLoadingFile = false,
                        waveformData = null,
                        errorMessage = null,
                        totalDurationMillis = 0L,
                        currentPositionMillis = 0L,
                        isPlaying = false,
                        isLoadingWaveform = true,
                        isPlayerLoading = true,
                        displayMinAmplitude = minAmp,
                        displayMaxAmplitude = maxAmp
                    )
                }

                // Single coroutine: player load + waveform are sequential (#9).
                // If loadAudioUseCase throws, the outer catch prevents waveform processing.
                viewModelScope.launch {
                    try {
                        stopAudioUseCase()
                        loadAudioUseCase(selectedUriString)

                        // Non-fatal: filename only; continue even on error (#6 — last error wins)
                        when (val detailsResult = getAudioTrackDetailsUseCase(selectedUriString)) {
                            is Result.Success ->
                                _viewState.update { it.copy(fileName = detailsResult.data.fileName) }
                            is Result.Error ->
                                _viewState.update { it.copy(errorMessage = detailsResult.message) }
                        }

                        applyWaveformResult(selectedUriString, segmentsToProcess)
                    } catch (e: Exception) {
                        _viewState.update { state ->
                            val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(
                                state.waveformData,
                                state.dynamicNormalizationEnabled
                            )
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
                    val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(
                        if (currentFileUri != null) null else it.waveformData,
                        it.dynamicNormalizationEnabled
                    )
                    it.copy(
                        currentNumSegments = newNumSegments,
                        isLoadingWaveform = if (currentFileUri != null) true else it.isLoadingWaveform,
                        waveformData = if (currentFileUri != null) null else it.waveformData,
                        displayMinAmplitude = minAmp,
                        displayMaxAmplitude = maxAmp
                    )
                }

                if (currentFileUri != null) {
                    viewModelScope.launch {
                        try {
                            applyWaveformResult(currentFileUri.toString(), newNumSegments)
                        } catch (e: Exception) {
                            _viewState.update { state ->
                                val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(
                                    state.waveformData,
                                    state.dynamicNormalizationEnabled
                                )
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
                    val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(
                        currentState.waveformData,
                        newNormalizationState
                    )
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
                            if (_viewState.value.totalDurationMillis > 0 &&
                                abs(_viewState.value.currentPositionMillis - _viewState.value.totalDurationMillis) < 500
                            ) {
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

            is WaveScreenIntent.SeekDragStarted -> {
                if (_viewState.value.isPlaying) pauseAudioUseCase()
                _viewState.update { it.copy(isSeeking = true) }
            }

            is WaveScreenIntent.SeekTo -> {
                val totalDuration = _viewState.value.totalDurationMillis
                if (totalDuration > 0 && !_viewState.value.isPlayerLoading) {
                    val newPosition = (totalDuration * intent.positionFraction).toLong()
                    seekAudioUseCase(newPosition)
                    playAudioUseCase()
                }
                _viewState.update { it.copy(isSeeking = false) }
            }

            is WaveScreenIntent.ClearErrorMessage -> {
                _viewState.update { it.copy(errorMessage = null) }
            }
        }
    }

    // Shared helper: checks cache, then calls use case, then updates state (#3 cache reuse)
    private suspend fun applyWaveformResult(uriString: String, numSegments: Int) {
        val cacheKey = CacheKey(uriString, numSegments)
        val cachedData = waveformCache[cacheKey]

        if (cachedData != null) {
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
                    displayMinAmplitude = minAmp,
                    displayMaxAmplitude = maxAmp,
                    isLoadingWaveform = false
                )
            }
            return
        }

        when (val result = getWaveformUseCase(uriString, numSegments)) {
            is Result.Success -> {
                waveformCache[cacheKey] = result.data
                _viewState.update { state ->
                    val newTotalDuration = if (state.totalDurationMillis == 0L) {
                        result.data.durationMillis.toLong()
                    } else {
                        state.totalDurationMillis
                    }
                    val (minAmp, maxAmp) = calculateDisplayAmplitudeRange(
                        result.data.waveformSegments,
                        state.dynamicNormalizationEnabled
                    )
                    state.copy(
                        waveformData = result.data.waveformSegments,
                        totalDurationMillis = newTotalDuration,
                        displayMinAmplitude = minAmp,
                        displayMaxAmplitude = maxAmp,
                        isLoadingWaveform = false
                    )
                }
            }
            is Result.Error -> {
                _viewState.update { state ->
                    state.copy(errorMessage = result.message, isLoadingWaveform = false)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayerUseCase()
        waveformCache.clear()
    }
}
