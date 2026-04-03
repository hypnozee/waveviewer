package com.waveform.data.remote

import com.waveform.data.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.storage.Storage

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

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY,
    ) {
        install(Functions)
        install(Auth)
        install(Storage)
    }
}
