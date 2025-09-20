package com.paradoxcat.waveformtest.domain.player.usecase

import android.net.Uri
import com.paradoxcat.waveformtest.domain.player.repository.AudioPlayer

/**
 * Use case for loading an audio file for playback.
 */
class LoadAudioUseCase(private val audioPlayer: AudioPlayer) {

    /**
     * Loads the audio file from the given URI.
     */
    suspend operator fun invoke(uri: Uri) {
        audioPlayer.load(uri)
    }
}
