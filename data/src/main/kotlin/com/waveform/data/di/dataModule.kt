package com.waveform.data.di

import com.waveform.data.player.AudioPlayerImpl
import com.waveform.data.remote.SupabaseClientProvider
import com.waveform.data.repository.AudioRepositoryImpl
import com.waveform.data.repository.AuthRepositoryImpl
import com.waveform.data.repository.RemoteAudioRepositoryImpl
import com.waveform.domain.player.repository.AudioPlayer
import com.waveform.domain.repository.AudioRepository
import com.waveform.domain.repository.AuthRepository
import com.waveform.domain.repository.RemoteAudioRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dataModule = module {
    // Existing
    single<AudioRepository> { AudioRepositoryImpl(androidApplication().contentResolver) }
    single<AudioPlayer> { AudioPlayerImpl(androidApplication()) }

    // Supabase client
    single { SupabaseClientProvider.client }

    // Remote repositories
    single<AuthRepository> { AuthRepositoryImpl(client = get()) }
    single<RemoteAudioRepository> { RemoteAudioRepositoryImpl(client = get()) }
}
