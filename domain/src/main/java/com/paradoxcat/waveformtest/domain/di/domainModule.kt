package com.paradoxcat.waveformtest.domain.di

import com.paradoxcat.waveformtest.domain.player.usecase.LoadAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.ObservePlaybackStateUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.PauseAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.PlayAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.ReleasePlayerUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.SeekAudioUseCase
import com.paradoxcat.waveformtest.domain.player.usecase.StopAudioUseCase
import com.paradoxcat.waveformtest.domain.usecase.GetAudioTrackDetailsUseCase
import com.paradoxcat.waveformtest.domain.usecase.GetWaveformUseCase
import org.koin.dsl.module
val domainModule = module {
    factory { LoadAudioUseCase(get()) }
    factory { PlayAudioUseCase(get()) }
    factory { PauseAudioUseCase(get()) }
    factory { SeekAudioUseCase(get()) }
    factory { StopAudioUseCase(get()) }
    factory { ObservePlaybackStateUseCase(get()) }
    factory { ReleasePlayerUseCase(get()) }
    factory { GetWaveformUseCase(get()) }
    factory { GetAudioTrackDetailsUseCase(get()) }
}
