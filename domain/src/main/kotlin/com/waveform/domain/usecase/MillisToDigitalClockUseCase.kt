package com.waveform.domain.usecase

import java.util.Locale

/**
 * Use case to format milliseconds into a classic audio duration format (MM:SS).
 */
class MillisToDigitalClockUseCase {

    operator fun invoke(millis: Long, locale: Locale = Locale.getDefault()): String {
        if (millis <= 0) {
            return "00:00"
        }
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60 + 1

        return String.format(locale, "%02d:%02d", minutes, seconds)
    }
}
