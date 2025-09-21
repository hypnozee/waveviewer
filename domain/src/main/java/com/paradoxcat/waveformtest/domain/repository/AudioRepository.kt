package com.paradoxcat.waveformtest.domain.repository

import com.paradoxcat.waveformtest.domain.model.AudioTrackDetails
import java.io.InputStream

/**
 * Interface for accessing audio file data.
 */
interface AudioRepository {

    /**
     * From a certain URI string, retrieve the audio as an InputStream
     */
    suspend fun getAudioFileInputStream(uriString: String): InputStream?

    /**
     * For a given URI string, retrieve the audio track details (name)
     */
    suspend fun getAudioTrackDetails(uriString: String): AudioTrackDetails?
}
