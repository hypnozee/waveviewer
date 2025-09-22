package com.paradoxcat.waveformtest.ui.di

import com.paradoxcat.waveformtest.ui.screen.WaveViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel {
        WaveViewModel(
            loadAudioUseCase = get(),
            playAudioUseCase = get(),
            pauseAudioUseCase = get(),
            seekAudioUseCase = get(),
            stopAudioUseCase = get(),
            observePlaybackStateUseCase = get(),
            releasePlayerUseCase = get(),
            getWaveformUseCase = get(),
            getAudioTrackDetailsUseCase = get(),
        )
    }
}
