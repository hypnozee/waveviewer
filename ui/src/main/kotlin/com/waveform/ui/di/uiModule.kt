package com.waveform.ui.di

import com.waveform.ui.screen.WaveViewModel
import com.waveform.ui.screen.auth.AuthViewModel
import com.waveform.ui.screen.files.RemoteFilesViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel {
        WaveViewModel(
            getWaveformUseCase = get(),
            getAudioTrackDetailsUseCase = get(),
            audioPlayerInteractor = get(),
            authInteractor = get(),
        )
    }

    viewModel {
        AuthViewModel(
            authInteractor = get(),
        )
    }

    viewModel {
        RemoteFilesViewModel(
            app = androidApplication(),
            authInteractor = get(),
            remoteFilesInteractor = get(),
        )
    }
}
