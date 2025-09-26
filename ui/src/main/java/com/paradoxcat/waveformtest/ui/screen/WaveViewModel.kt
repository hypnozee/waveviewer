package com.paradoxcat.waveformtest.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradoxcat.waveformtest.domain.core.Result
import com.paradoxcat.waveformtest.domain.model.WaveformResultData
import com.paradoxcat.waveformtest.domain.player.usecase.LoadAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.ObservePlaybackStateUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.PauseAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.PlayAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.ReleasePlayerUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.SeekAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.StopAudioUseCase
import com.paradoxcat.waveformtest.domain.usecase.MillisToDigitalClockUseCase
import com.paradoxcat.waveformtest.domain.usecase.GetAudioTrackDetailsUseCase
import com.paradoxcat.waveformtest.domain.usecase.GetWaveformUseCase
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

    private val _viewState =
        MutableStateFlow(WaveScreenState())
    val viewStateFlow = _viewState.asStateFlow()

    private val waveformCache = mutableMapOf<String, WaveformResultData>()

    init {
        viewModelScope.launch {
            var previousIsPlaying = false
            observePlaybackStateUseCase().collect { playbackState ->
                _viewState.update { currentState ->
                    val latestErrorMessage = playbackState.error ?: currentState.errorMessage
                    val currentFileUriString = currentState.fileUri?.toString()
                    val cachedDataForUri =
                        if (currentFileUriString != null) waveformCache[currentFileUriString] else null

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
                        newCurrentPositionMillis = 0L // Reset to start
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

    fun processIntent(intent: WaveScreenIntent) {
        when (intent) {
            is WaveScreenIntent.PickFileClicked -> {
                _viewState.update {
                    it.copy(
                        errorMessage = null // Clear previous errors
                    )
                }
            }

            is WaveScreenIntent.FileSelected -> {
                val selectedUri = intent.uri
                val selectedUriString = selectedUri.toString()
                val segmentsToProcess = _viewState.value.currentTargetSegments

                _viewState.update {
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
                                            errorMessage = if (currentError != null && !currentError.contains(
                                                    newError
                                                )
                                            ) "$currentError\n$newError" else newError
                                        )
                                    }
                                }
                            }
                        }
                        // Always fetch with current target segments. Cache will be updated.
                        when (val waveformResult =
                            getWaveformUseCase(selectedUriString, segmentsToProcess)) {
                            is Result.Success -> {
                                waveformCache[selectedUriString] =
                                    waveformResult.data // Update cache
                                _viewState.update { state ->
                                    val newTotalDuration = if (state.totalDurationMillis == 0L) waveformResult.data.durationMillis.toLong() else state.totalDurationMillis
                                    state.copy(
                                        waveformData = waveformResult.data.waveformSegments,
                                        totalDurationMillis = newTotalDuration,
                                        totalAudioDuration = millisToDigitalClockUseCase(newTotalDuration)
                                    )
                                }
                            }

                            is Result.Error -> {
                                _viewState.update { state ->
                                    val currentError = state.errorMessage
                                    val newError = waveformResult.message
                                    state.copy(
                                        errorMessage = if (currentError != null && !currentError.contains(
                                                newError
                                            )
                                        ) "$currentError\n$newError" else newError
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        _viewState.update {
                            it.copy(
                                errorMessage = "An unexpected error occurred: ${e.message}",
                                isPlayerLoading = false
                            )
                        }
                    } finally {
                        _viewState.update { it.copy(isLoadingWaveform = false) }
                    }
                }
            }

            is WaveScreenIntent.TargetSegmentsChanged -> {
                val newSegmentCount =
                    intent.newSegmentCount.coerceIn(MIN_TARGET_SEGMENTS, MAX_TARGET_SEGMENTS)
                val currentFileUri = _viewState.value.fileUri

                _viewState.update {
                    it.copy(
                        currentTargetSegments = newSegmentCount,
                        isLoadingWaveform = if (currentFileUri != null) true else it.isLoadingWaveform,
                        waveformData = if (currentFileUri != null) null else it.waveformData 
                    )
                }

                if (currentFileUri != null) {
                    val currentFileUriString = currentFileUri.toString()
                    viewModelScope.launch {
                        try {
                            when (val waveformResult =
                                getWaveformUseCase(currentFileUriString, newSegmentCount)) {
                                is Result.Success -> {
                                    waveformCache[currentFileUriString] =
                                        waveformResult.data 
                                    _viewState.update { state ->
                                        val newTotalDuration = if (state.totalDurationMillis == 0L || waveformCache[currentFileUriString]?.durationMillis?.toLong() != state.totalDurationMillis) {
                                            waveformResult.data.durationMillis.toLong()
                                        } else {
                                            state.totalDurationMillis
                                        }
                                        state.copy(
                                            waveformData = waveformResult.data.waveformSegments,
                                            totalDurationMillis = newTotalDuration,
                                            totalAudioDuration = millisToDigitalClockUseCase(newTotalDuration)
                                        )
                                    }
                                }

                                is Result.Error -> {
                                    _viewState.update { state ->
                                        val currentError = state.errorMessage
                                        val newError = waveformResult.message
                                        state.copy(
                                            errorMessage = if (currentError != null && !currentError.contains(
                                                    newError
                                                )
                                            ) "$currentError\n$newError" else newError
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            _viewState.update {
                                it.copy(errorMessage = "Error re-processing waveform: ${e.message}")
                            }
                        } finally {
                            _viewState.update { it.copy(isLoadingWaveform = false) }
                        }
                    }
                }
            }

            is WaveScreenIntent.PlayPauseClicked -> {
                if (_viewState.value.isPlaying) {
                    pauseAudioUseCase()
                } else {
                    if (_viewState.value.fileUri != null) {
                        if (!_viewState.value.isPlayerLoading) {
                            // If at the end, restart playback
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

            is WaveScreenIntent.ToggleDynamicNormalization -> {
                _viewState.update { it.copy(dynamicNormalizationEnabled = !it.dynamicNormalizationEnabled) }
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
