package com.waveform.domain.model

/**
 * Represents the current authentication state.
 */
sealed class AuthState {
    /** Session status is still being determined. */
    data object Loading : AuthState()

    /** User is authenticated. */
    data class Authenticated(val user: UserInfo) : AuthState()

    /** No active session. */
    data object NotAuthenticated : AuthState()
}
