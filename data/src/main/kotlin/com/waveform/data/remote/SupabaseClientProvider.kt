package com.waveform.data.remote

import com.waveform.data.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.annotations.SupabaseInternal
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import android.util.Log

/**
 * Provides a configured [SupabaseClient] singleton.
 *
 * Reads credentials from BuildConfig, which are populated
 * from local.properties at build time.
 *
 * Uses Edge Functions for all API calls — the app does not
 * query the database directly.
 */
object SupabaseClientProvider {

    @OptIn(SupabaseInternal::class)
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY,
    ) {
        defaultLogLevel = io.github.jan.supabase.logging.LogLevel.DEBUG
        install(Functions)
        install(Auth)
        install(Storage)

        httpConfig {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("Supabase-HTTP", message)
                    }
                }
                level = LogLevel.ALL
            }
        }
    }
}
