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
import com.waveform.domain.usecase.GetAudioTrackDetailsUseCase
import com.waveform.domain.usecase.GetWaveformUseCase
import org.koin.dsl.module

val domainModule = module {
    single { WavHeaderParser() }
    single { WavStreamProcessor() }

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
}
