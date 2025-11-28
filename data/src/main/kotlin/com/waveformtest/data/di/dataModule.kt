package com.waveformtest.data.di

import com.waveformtest.data.player.AudioPlayerImpl
import com.waveformtest.data.repository.AudioRepositoryImpl
import com.waveformtest.domain.player.repository.AudioPlayer
import com.waveformtest.domain.repository.AudioRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dataModule = module {
    single<AudioRepository> { AudioRepositoryImpl(androidApplication().contentResolver) }
    single<AudioPlayer> { AudioPlayerImpl(androidApplication()) }
}
