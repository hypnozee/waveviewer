package com.waveform.domain.usecase

import com.waveform.domain.repository.AuthRepository

class SignOutUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke() {
        authRepository.signOut()
    }
}
