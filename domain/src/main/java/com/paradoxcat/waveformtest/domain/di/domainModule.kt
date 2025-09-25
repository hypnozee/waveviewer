package com.paradoxcat.waveformtest.domain.di

import com.paradoxcat.waveformtest.domain.player.repository.AudioPlayer
import com.paradoxcat.waveformtest.domain.player.usecase.LoadAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.ObservePlaybackStateUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.PauseAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.PlayAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.ReleasePlayerUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.SeekAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.StopAudioUseCase
import com.paradoxcat.waveformtest.domain.repository.AudioRepository
import com.paradoxcat.waveformtest.domain.usecase.GetAudioTrackDetailsUseCase
import com.paradoxcat.waveformtest.domain.usecase.GetWaveformUseCase
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
}
