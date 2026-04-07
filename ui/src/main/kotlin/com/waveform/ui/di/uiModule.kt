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
            loadAudioUseCase = get(),
            playAudioUseCase = get(),
            pauseAudioUseCase = get(),
            seekAudioUseCase = get(),
            stopAudioUseCase = get(),
            observePlaybackStateUseCase = get(),
            releasePlayerUseCase = get(),
            getWaveformUseCase = get(),
            getAudioTrackDetailsUseCase = get(),
            observeAuthStateUseCase = get(),
            signOutUseCase = get(),
        )
    }

    viewModel {
        AuthViewModel(
            signInUseCase = get(),
            signUpUseCase = get(),
            verifyOtpUseCase = get(),
        )
    }

    viewModel {
        RemoteFilesViewModel(
            app = androidApplication(),
            observeAuthStateUseCase = get(),
            getPublicAudioFilesUseCase = get(),
            getUserAudioFilesUseCase = get(),
            downloadAudioFileUseCase = get(),
            uploadAudioFileUseCase = get(),
            deleteAudioFileUseCase = get(),
        )
    }
}
