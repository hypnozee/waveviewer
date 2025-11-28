package com.waveform.data.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import com.waveform.domain.model.AudioTrackDetails
import com.waveform.domain.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Gets audio file data.
 */
class AudioRepositoryImpl(
    private val contentResolver: ContentResolver,
) : AudioRepository {

    @SuppressLint("Recycle")
    override suspend fun getAudioFileInputStream(uriString: String): InputStream? {
        return withContext(Dispatchers.IO) {
            try {
                if (uriString.isEmpty()) {
                    Log.e("AudioRepositoryImpl", "URI string is empty, cannot open input stream.")
                    return@withContext null
                }
                contentResolver.openInputStream(uriString.toUri())
            } catch (e: Exception) {
                Log.e("AudioRepositoryImpl", "Error opening input stream for URI: $uriString", e)
                null
            }
        }
    }

    override suspend fun getAudioTrackDetails(uriString: String): AudioTrackDetails? {
        return withContext(Dispatchers.IO) {
            if (uriString.isEmpty()) {
                Log.w("AudioRepositoryImpl", "URI string is empty, cannot get track details.")
                return@withContext null
            }
            val uri = uriString.toUri()
            var fileName: String? = null

            if (uri.scheme == "content") {
                try {
                    val cursor: Cursor? = contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (displayNameIndex != -1) {
                                fileName = it.getString(displayNameIndex)
                                Log.d(
                                    "AudioRepositoryImpl",
                                    "File name from ContentResolver: $fileName for URI: $uri"
                                )
                            } else {
                                Log.w(
                                    "AudioRepositoryImpl",
                                    "DISPLAY_NAME column not found for URI: $uri"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AudioRepositoryImpl", "Error querying ContentResolver for URI: $uri", e)
                }
            }

            if (fileName == null) {
                uri.path?.let { path ->
                    val cut = path.lastIndexOf('/')
                    if (cut != -1) {
                        fileName = path.substring(cut + 1)
                        Log.d(
                            "AudioRepositoryImpl",
                            "File name from URI path: $fileName for URI: $uri"
                        )
                    } else {
                        fileName = path
                        Log.d(
                            "AudioRepositoryImpl",
                            "File name from URI path (no slash): $fileName for URI: $uri"
                        )
                    }
                }
            }

            if (fileName != null) {
                AudioTrackDetails(fileName = fileName!!)
            } else {
                Log.w("AudioRepositoryImpl", "Could not determine file name for URI: $uri")
                null
            }
        }
    }
}
