package com.waveform.domain.repository

import com.waveform.domain.core.Result
import com.waveform.domain.model.AuthState
import com.waveform.domain.model.UserInfo
import kotlinx.coroutines.flow.Flow

/**
 * Interface for authentication operations.
 */
interface AuthRepository {

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(email: String, password: String): Result<UserInfo>

    /**
     * Create a new account with email and password.
     */
    suspend fun signUp(email: String, password: String): Result<UserInfo>

    /**
     * Sign out the current user.
     */
    suspend fun signOut()

    /**
     * Get the currently authenticated user, or null if not signed in.
     */
    fun getCurrentUser(): UserInfo?

    /**
     * Observe the authentication state as a Flow.
     */
    fun observeAuthState(): Flow<AuthState>
}
