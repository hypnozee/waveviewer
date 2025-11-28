package com.waveformtest.domain.di

import com.waveformtest.domain.player.repository.AudioPlayer
import com.waveformtest.domain.player.usecase.LoadAudioUseCase
import com.waveformtest.domain.player.usecase.ObservePlaybackStateUseCase
import com.waveformtest.domain.player.usecase.PauseAudioUseCase
import com.waveformtest.domain.player.usecase.PlayAudioUseCase
import com.waveformtest.domain.player.usecase.ReleasePlayerUseCase
import com.waveformtest.domain.player.usecase.SeekAudioUseCase
import com.waveformtest.domain.player.usecase.StopAudioUseCase
import com.waveformtest.domain.repository.AudioRepository
import com.waveformtest.domain.usecase.MillisToDigitalClockUseCase
import com.waveformtest.domain.usecase.GetAudioTrackDetailsUseCase
import com.waveformtest.domain.usecase.GetWaveformUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { LoadAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { PlayAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { PauseAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { SeekAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { StopAudioUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { ObservePlaybackStateUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { ReleasePlayerUseCase(audioPlayer = get<AudioPlayer>()) }
    factory { GetWaveformUseCase(audioRepository = get<AudioRepository>()) }
    factory { GetAudioTrackDetailsUseCase(audioRepository = get<AudioRepository>()) }
    factory { MillisToDigitalClockUseCase() }
}
