package com.waveform.ui.screen.auth

data class AuthScreenState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val otpCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Non-null while waiting for the user to enter the OTP sent after sign-up. */
    val pendingOtpEmail: String? = null,
)

enum class AuthMode { LOGIN, SIGN_UP }
