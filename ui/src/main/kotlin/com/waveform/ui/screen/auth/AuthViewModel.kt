package com.waveform.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveform.domain.core.Result
import com.waveform.domain.usecase.SignInUseCase
import com.waveform.domain.usecase.SignUpUseCase
import com.waveform.domain.usecase.VerifyOtpUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthScreenState())
    val state = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onModeChanged(mode: AuthMode) {
        _state.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun onOtpCodeChanged(otpCode: String) {
        _state.update { it.copy(otpCode = otpCode, errorMessage = null) }
    }

    fun submit() {
        when (_state.value.mode) {
            AuthMode.LOGIN -> signIn()
            AuthMode.SIGN_UP -> signUp()
        }
    }

    fun verifyOtp() {
        val email = _state.value.pendingOtpEmail ?: return
        val token = _state.value.otpCode.trim()
        if (token.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter the verification code.") }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = verifyOtpUseCase(email, token)) {
                is Result.Success -> _events.send(AuthEvent.Success)
                is Result.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    private fun signIn() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Email and password are required.") }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = signInUseCase(s.email.trim(), s.password)) {
                is Result.Success -> _events.send(AuthEvent.Success)
                is Result.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    private fun signUp() {
        val s = _state.value
        validatePassword(s.password)?.let { error ->
            _state.update { it.copy(errorMessage = error) }
            return
        }
        if (s.password != s.confirmPassword) {
            _state.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = signUpUseCase(s.email.trim(), s.password)) {
                is Result.Success -> {
                    // OTP sent — move to verification step
                    _state.update { it.copy(isLoading = false, pendingOtpEmail = s.email.trim()) }
                }
                is Result.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters."
        if (password.none { it.isUpperCase() }) return "Password must contain at least one uppercase letter."
        if (password.none { it.isDigit() }) return "Password must contain at least one digit."
        return null
    }
}

sealed interface AuthEvent {
    data object Success : AuthEvent
}
