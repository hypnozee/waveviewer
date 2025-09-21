package com.paradoxcat.waveformtest.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradoxcat.waveformtest.domain.model.WaveformResultData
import com.paradoxcat.waveformtest.domain.player.usecase.LoadAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.ObservePlaybackStateUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.PauseAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.PlayAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.ReleasePlayerUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.SeekAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.StopAudioUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WaveViewModel(
    private val loadAudioUseCase: LoadAudioUseCase,
    private val playAudioUseCase: PlayAudioUseCase,
    private val pauseAudioUseCase: PauseAudioUseCase,
    private val seekAudioUseCase: SeekAudioUseCase,
    private val stopAudioUseCase: StopAudioUseCase,
    private val observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    private val releasePlayerUseCase: ReleasePlayerUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaveScreenState())
    val uiState = _uiState.asStateFlow()
    private val waveformCache = mutableMapOf<String, WaveformResultData>()

    init {
        viewModelScope.launch {
            observePlaybackStateUseCase().collect { playbackState ->
                _uiState.update { currentState ->
                    val finalErrorMessage = playbackState.error ?: currentState.errorMessage
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
                        errorMessage = finalErrorMessage
                    )
                }
            }
        }
    }

    fun onEvent(event: WaveScreenEvent) {
        when (event) {
            is WaveScreenEvent.PickFileClicked -> {
                _uiState.update {
                    it.copy(
                        isLoadingFile = true,
                        errorMessage = null
                    )
                }
            }

            is WaveScreenEvent.FileSelected -> {
                val selectedUri = event.uri
                val cacheKey = selectedUri.toString()

                _uiState.update {
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
                        isPlayerLoading = true
                    )
                }
            }

            is WaveScreenEvent.PlayPauseClicked -> {
                if (_uiState.value.isPlaying) {
                    pauseAudioUseCase()
                } else {
                    if (_uiState.value.fileUri != null) {
                        if (!_uiState.value.isPlayerLoading) {
                            playAudioUseCase()
                        } else {
                            _uiState.update { it.copy(errorMessage = "Player is still loading the audio.") }
                        }
                    } else {
                        _uiState.update { it.copy(errorMessage = "No audio file loaded.") }
                    }
                }
            }

            is WaveScreenEvent.SeekTo -> {
                val totalDuration = _uiState.value.totalDurationMillis
                if (totalDuration > 0 && !_uiState.value.isPlayerLoading) {
                    val newPosition = (totalDuration * event.positionFraction).toLong()
                    seekAudioUseCase(newPosition)
                }
            }

            is WaveScreenEvent.ToggleDynamicNormalization -> {
                _uiState.update { it.copy(dynamicNormalizationEnabled = !it.dynamicNormalizationEnabled) }
            }

            is WaveScreenEvent.ErrorMessageShown -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayerUseCase()
        waveformCache.clear()
    }
}
