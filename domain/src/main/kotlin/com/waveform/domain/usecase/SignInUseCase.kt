package com.waveform.domain.usecase

import com.waveform.domain.core.Result
import com.waveform.domain.model.UserInfo
import com.waveform.domain.repository.AuthRepository

class SignInUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): Result<UserInfo> {
        return try {
            authRepository.signIn(email, password)
        } catch (e: Exception) {
            Result.Error("Sign in failed: ${e.message}")
        }
    }
}
