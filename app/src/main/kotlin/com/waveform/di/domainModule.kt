package com.waveform.di

import com.waveform.domain.auth.AuthInteractor
import com.waveform.domain.parser.WavHeaderParser
import com.waveform.domain.player.AudioPlayerInteractor
import com.waveform.domain.player.repository.AudioPlayer
import com.waveform.domain.player.usecase.LoadAudioUseCase
import com.waveform.domain.player.usecase.ObservePlaybackStateUseCase
import com.waveform.domain.player.usecase.PauseAudioUseCase
import com.waveform.domain.player.usecase.PlayAudioUseCase
import com.waveform.domain.player.usecase.ReleasePlayerUseCase
import com.waveform.domain.player.usecase.SeekAudioUseCase
import com.waveform.domain.player.usecase.StopAudioUseCase
import com.waveform.domain.processor.WavStreamProcessor
import com.waveform.domain.remote.RemoteFilesInteractor
import com.waveform.domain.repository.AudioRepository
import com.waveform.domain.repository.AuthRepository
import com.waveform.domain.repository.RemoteAudioRepository
import com.waveform.domain.usecase.DeleteAudioFileUseCase
import com.waveform.domain.usecase.DownloadAudioFileUseCase
import com.waveform.domain.usecase.GetAudioTrackDetailsUseCase
import com.waveform.domain.usecase.GetPublicAudioFilesUseCase
import com.waveform.domain.usecase.GetUserAudioFilesUseCase
import com.waveform.domain.usecase.GetWaveformUseCase
import com.waveform.domain.usecase.ObserveAuthStateUseCase
import com.waveform.domain.usecase.SignInUseCase
import com.waveform.domain.usecase.SignOutUseCase
import com.waveform.domain.usecase.SignUpUseCase
import com.waveform.domain.usecase.UploadAudioFileUseCase
import com.waveform.domain.usecase.VerifyOtpUseCase
import org.koin.dsl.module

val domainModule = module {
    single(createdAtStart = true) { WavHeaderParser() }
    single(createdAtStart = true) { WavStreamProcessor() }

    // Interactors (Added to resolve DEPENDENCY_COMPLEXITY)
    single { AudioPlayerInteractor(audioPlayer = get()) }
    single { AuthInteractor(authRepository = get()) }
    single { RemoteFilesInteractor(remoteAudioRepository = get()) }

    // Existing audio player use cases
    single { LoadAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    single { PlayAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    single { PauseAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    single { SeekAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    single { StopAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    single { ObservePlaybackStateUseCase(audioPlayer = get<AudioPlayer>()) }
    single { ReleasePlayerUseCase(audioPlayer = get<AudioPlayer>()) }
    single {
        GetWaveformUseCase(
            audioRepository = get<AudioRepository>(),
            wavHeaderParser = get(),
            wavStreamProcessor = get(),
        )
    }
    single { GetAudioTrackDetailsUseCase(audioRepository = get<AudioRepository>()) }

    // Auth use cases
    single { SignInUseCase(authRepository = get<AuthRepository>()) }
    single { SignUpUseCase(authRepository = get<AuthRepository>()) }
    single { SignOutUseCase(authRepository = get<AuthRepository>()) }
    single { VerifyOtpUseCase(authRepository = get<AuthRepository>()) }
    single { ObserveAuthStateUseCase(authRepository = get<AuthRepository>()) }

    // Remote audio file use cases
    single { GetPublicAudioFilesUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
    single { GetUserAudioFilesUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
    single { UploadAudioFileUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
    single { DeleteAudioFileUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
    single { DownloadAudioFileUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
}
