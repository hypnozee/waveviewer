package com.waveform.domain.model

/**
 * Domain model representing an authenticated user.
 */
data class UserInfo(
    val id: String,
    val email: String?,
)
