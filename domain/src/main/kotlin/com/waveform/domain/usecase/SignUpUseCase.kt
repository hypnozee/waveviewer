package com.waveform.domain.usecase

import com.waveform.domain.core.Result
import com.waveform.domain.model.UserInfo
import com.waveform.domain.repository.AuthRepository

class SignUpUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): Result<UserInfo> {
        return try {
            authRepository.signUp(email, password)
        } catch (e: Exception) {
            Result.Error("Sign up failed: ${e.message}")
        }
    }
}
