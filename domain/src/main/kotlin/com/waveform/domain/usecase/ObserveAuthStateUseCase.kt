package com.waveform.domain.usecase

import com.waveform.domain.model.AuthState
import com.waveform.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase(private val authRepository: AuthRepository) {

    operator fun invoke(): Flow<AuthState> {
        return authRepository.observeAuthState()
    }
}
