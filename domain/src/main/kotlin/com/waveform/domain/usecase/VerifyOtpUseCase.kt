package com.waveform.domain.usecase

import com.waveform.domain.core.Result
import com.waveform.domain.model.UserInfo
import com.waveform.domain.repository.AuthRepository

class VerifyOtpUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, token: String): Result<UserInfo> {
        return try {
            authRepository.verifyOtp(email, token)
        } catch (e: Exception) {
            Result.Error("OTP verification failed: ${e.message}")
        }
    }
}
