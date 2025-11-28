package com.waveform.domain.core

/**
 * Handle the outcome of audio operations.
 * Either [Success] with result data,
 * Or [Error] with a message.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
