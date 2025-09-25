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
import com.paradoxcat.waveformtest.domain.usecase.GetAudioTrackDetailsUseCase
import com.paradoxcat.waveformtest.domain.usecase.GetWaveformUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private val viewState = MutableStateFlow(WaveScreenState())
    val viewStateFlow = viewState.asStateFlow()
    private val waveformCache = mutableMapOf<String, WaveformResultData>()

    init {
        viewModelScope.launch {
            observePlaybackStateUseCase().collect { playbackState ->
                viewState.update { currentState ->
                    val latestErrorMessage = playbackState.error ?: currentState.errorMessage
                    val resolvedTotalDuration = if (playbackState.totalDurationMillis == 0L &&
                        currentState.fileUri != null &&
                        waveformCache[currentState.fileUri.toString()] != null
                    ) {
                        waveformCache[currentState.fileUri.toString()]!!.durationMillis.toLong()
                    } else {
                        playbackState.totalDurationMillis
                    }
                    currentState.copy(
                        isPlayerLoading = playbackState.isLoading,
                        isPlaying = playbackState.isPlaying,
                        currentPositionMillis = playbackState.currentPositionMillis,
                        totalDurationMillis = resolvedTotalDuration,
                        errorMessage = if (playbackState.error != null) latestErrorMessage else currentState.errorMessage
                    )
                }
            }
        }
    }

    fun processIntent(intent: WaveScreenIntent) {
        when (intent) {
            is WaveScreenIntent.PickFileClicked -> {
                viewState.update {
                    it.copy(
                        errorMessage = null
                    )
                }
            }

            is WaveScreenIntent.FileSelected -> {
                val selectedUri = intent.uri
                val cacheKey = selectedUri.toString()

                viewState.update {
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
                    )
                }

                viewModelScope.launch {
                    try {
                        stopAudioUseCase() // Stop any previous playback
                        loadAudioUseCase(selectedUri.toString()) // Load new audio into player

                        // Fetch track details
                        when (val detailsResult =
                            getAudioTrackDetailsUseCase(selectedUri.toString())) {
                            is Result.Success -> {
                                viewState.update { it.copy(fileName = detailsResult.data.fileName) }
                            }

                            is Result.Error -> {
                                viewState.update { state ->
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

                        // Get waveform data
                        val cachedWaveformResult = waveformCache[cacheKey]
                        if (cachedWaveformResult != null) {
                            viewState.update {
                                it.copy(
                                    waveformData = cachedWaveformResult.waveformSegments,
                                    totalDurationMillis = if (it.totalDurationMillis == 0L) cachedWaveformResult.durationMillis.toLong() else it.totalDurationMillis,
                                )
                            }
                        } else {
                            when (val waveformResult = getWaveformUseCase(selectedUri.toString())) {
                                is Result.Success -> {
                                    val resultToCache = WaveformResultData(
                                        waveformSegments = waveformResult.data.waveformSegments,
                                        durationMillis = waveformResult.data.durationMillis
                                    )
                                    waveformCache[cacheKey] = resultToCache
                                    viewState.update { state ->
                                        state.copy(
                                            waveformData = waveformResult.data.waveformSegments,
                                            totalDurationMillis = if (state.totalDurationMillis == 0L) waveformResult.data.durationMillis.toLong() else state.totalDurationMillis,
                                        )
                                    }
                                }

                                is Result.Error -> {
                                    viewState.update { state ->
                                        val currentError = state.errorMessage
                                        val newError = waveformResult.message
                                        state.copy(
                                            errorMessage = if (currentError != null && !currentError.contains(
                                                    newError
                                                )
                                            ) "$currentError\n$newError" else newError,
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        viewState.update {
                            it.copy(
                                errorMessage = "An unexpected error occurred: ${e.message}",
                                isPlayerLoading = false // Stop player loading on error
                            )
                        }
                    } finally {
                        viewState.update { it.copy(isLoadingWaveform = false) }
                    }
                }
            }

            is WaveScreenIntent.PlayPauseClicked -> {
                if (viewState.value.isPlaying) {
                    pauseAudioUseCase()
                } else {
                    if (viewState.value.fileUri != null) {
                        if (!viewState.value.isPlayerLoading) { // Check if player is ready
                            playAudioUseCase()
                        } else {
                            viewState.update { it.copy(errorMessage = "Player is still loading the audio.") }
                        }
                    } else {
                        viewState.update { it.copy(errorMessage = "No audio file loaded.") }
                    }
                }
            }

            is WaveScreenIntent.SeekTo -> {
                val totalDuration = viewState.value.totalDurationMillis
                if (totalDuration > 0 && !viewState.value.isPlayerLoading) {
                    val newPosition = (totalDuration * intent.positionFraction).toLong()
                    seekAudioUseCase(newPosition)
                }
            }

            is WaveScreenIntent.ToggleDynamicNormalization -> {
                viewState.update { it.copy(dynamicNormalizationEnabled = !it.dynamicNormalizationEnabled) }
            }

            is WaveScreenIntent.ClearErrorMessage -> {
                viewState.update { it.copy(errorMessage = null) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayerUseCase()
        waveformCache.clear()
    }
}
