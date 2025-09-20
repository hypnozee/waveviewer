package com.paradoxcat.waveformtest.domain.repository

import android.net.Uri
import com.paradoxcat.waveformtest.domain.model.AudioTrackDetails
import java.io.InputStream

/**
 * Interface for accessing audio file data.
 */
interface AudioRepository {

    /**
     * From a certain URI, retrieve the audio as an InputStream
     */
    suspend fun getAudioFileInputStream(uri: Uri): InputStream?

    /**
     * For a given URI, retrieve the audio track details (name)
     */
    suspend fun getAudioTrackDetails(uri: Uri): AudioTrackDetails?
}
