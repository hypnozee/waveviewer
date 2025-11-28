package com.waveform.data.di

import com.waveform.data.player.AudioPlayerImpl
import com.waveform.data.repository.AudioRepositoryImpl
import com.waveform.domain.player.repository.AudioPlayer
import com.waveform.domain.repository.AudioRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dataModule = module {
    single<AudioRepository> { AudioRepositoryImpl(androidApplication().contentResolver) }
    single<AudioPlayer> { AudioPlayerImpl(androidApplication()) }
}
