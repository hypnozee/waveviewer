package com.paradoxcat.waveformtest.data.di

import com.paradoxcat.waveformtest.data.player.AudioPlayerImpl
import com.paradoxcat.waveformtest.data.repository.AudioRepositoryImpl
import com.paradoxcat.waveformtest.domain.player.repository.AudioPlayer
import com.paradoxcat.waveformtest.domain.repository.AudioRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dataModule = module {
    single<AudioRepository> { AudioRepositoryImpl(androidApplication().contentResolver) }
    single<AudioPlayer> { AudioPlayerImpl(androidApplication()) }
}
