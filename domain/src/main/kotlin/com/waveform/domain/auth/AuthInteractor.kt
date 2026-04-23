package com.waveform.domain.auth

import com.waveform.domain.core.Result
import com.waveform.domain.model.AuthState
import com.waveform.domain.model.UserInfo
import com.waveform.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Interactor that groups all authentication related operations.
 */
class AuthInteractor(
    private val authRepository: AuthRepository
) {
    fun observeAuthState(): Flow<AuthState> = authRepository.observeAuthState()

    suspend fun signIn(email: String, password: String): Result<UserInfo> =
        authRepository.signIn(email, password)

    suspend fun signUp(email: String, password: String): Result<UserInfo> =
        authRepository.signUp(email, password)

    suspend fun verifyOtp(email: String, token: String): Result<UserInfo> =
        authRepository.verifyOtp(email, token)

    suspend fun signOut() {
        authRepository.signOut()
    }
}
