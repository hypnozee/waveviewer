package com.waveform.di

import com.waveform.domain.parser.WavHeaderParser
import com.waveform.domain.player.repository.AudioPlayer
import com.waveform.domain.player.usecase.LoadAudioUseCase
import com.waveform.domain.player.usecase.ObservePlaybackStateUseCase
import com.waveform.domain.player.usecase.PauseAudioUseCase
import com.waveform.domain.player.usecase.PlayAudioUseCase
import com.waveform.domain.player.usecase.ReleasePlayerUseCase
import com.waveform.domain.player.usecase.SeekAudioUseCase
import com.waveform.domain.player.usecase.StopAudioUseCase
import com.waveform.domain.processor.WavStreamProcessor
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
    single { WavHeaderParser() }
    single { WavStreamProcessor() }

    // Existing audio player use cases
    factory { LoadAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { PlayAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { PauseAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { SeekAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { StopAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { ObservePlaybackStateUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { ReleasePlayerUseCase(audioPlayer = get<AudioPlayer>()) }
    factory {
        GetWaveformUseCase(
            audioRepository = get<AudioRepository>(),
            wavHeaderParser = get(),
            wavStreamProcessor = get(),
        )
    }
    factory { GetAudioTrackDetailsUseCase(audioRepository = get<AudioRepository>()) }

    // Auth use cases
    factory { SignInUseCase(authRepository = get<AuthRepository>()) }
    factory { SignUpUseCase(authRepository = get<AuthRepository>()) }
    factory { SignOutUseCase(authRepository = get<AuthRepository>()) }
    factory { VerifyOtpUseCase(authRepository = get<AuthRepository>()) }
    factory { ObserveAuthStateUseCase(authRepository = get<AuthRepository>()) }

    // Remote audio file use cases
    factory { GetPublicAudioFilesUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
    factory { GetUserAudioFilesUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
    factory { UploadAudioFileUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
    factory { DeleteAudioFileUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
    factory { DownloadAudioFileUseCase(remoteAudioRepository = get<RemoteAudioRepository>()) }
}
