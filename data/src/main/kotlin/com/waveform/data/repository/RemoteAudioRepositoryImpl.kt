package com.waveform.data.repository

import android.util.Log
import com.waveform.data.remote.dto.AudioFileDto
import com.waveform.data.remote.dto.DeleteRequest
import com.waveform.data.remote.dto.DownloadRequest
import com.waveform.data.remote.dto.UploadRequest
import com.waveform.data.remote.mapper.AudioFileMapper.toDomain
import com.waveform.domain.core.Result
import com.waveform.domain.model.AudioFileInfo
import com.waveform.domain.repository.RemoteAudioRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Supabase-backed implementation of [RemoteAudioRepository].
 *
 * All data operations go through Edge Functions — the client never
 * queries the database directly. The Edge Functions handle DB access,
 * validation, and storage operations server-side.
 *
 * Each Edge Function is called via:
 *   POST https://<project-ref>.supabase.co/functions/v1/<function-slug>
 *
 * The supabase-kt client automatically attaches:
 *   - Authorization: Bearer <access_token> (if authenticated)
 *   - apikey: <anon_key>
 */
class RemoteAudioRepositoryImpl(
    private val client: SupabaseClient,
) : RemoteAudioRepository {

    companion object {
        private const val TAG = "RemoteAudioRepo"

        // Edge Function slugs — must match the deployed function names in Supabase.
        private const val FN_PUBLIC_LIST = "audio-public-list"
        private const val FN_USER_LIST = "audio-user-list"
        private const val FN_USER_UPLOAD = "audio-user-upload"
        private const val FN_USER_DELETE = "audio-user-delete"
        private const val FN_DOWNLOAD = "audio-download"
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getPublicAudioFiles(): Result<List<AudioFileInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.functions.invoke(FN_PUBLIC_LIST)
                val dtos = json.decodeFromString<List<AudioFileDto>>(response.body<String>())
                Result.Success(dtos.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch public audio files", e)
                Result.Error("Failed to fetch public audio files: ${e.message}")
            }
        }
    }

    override suspend fun getUserAudioFiles(): Result<List<AudioFileInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.functions.invoke(FN_USER_LIST)
                val dtos = json.decodeFromString<List<AudioFileDto>>(response.body<String>())
                Result.Success(dtos.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch user audio files", e)
                Result.Error("Failed to fetch user audio files: ${e.message}")
            }
        }
    }

    override suspend fun uploadAudioFile(
        name: String,
        mimeType: String,
        bytes: ByteArray,
    ): Result<AudioFileInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = UploadRequest(
                    name = name,
                    mimeType = mimeType,
                    sizeBytes = bytes.size.toLong(),
                    fileData = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP),
                )
                val response = client.functions.invoke(
                    function = FN_USER_UPLOAD,
                    body = requestBody,
                )
                val dto = json.decodeFromString<AudioFileDto>(response.body<String>())
                Result.Success(dto.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload audio file", e)
                Result.Error("Failed to upload audio file: ${e.message}")
            }
        }
    }

    override suspend fun deleteAudioFile(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.functions.invoke(
                    function = FN_USER_DELETE,
                    body = DeleteRequest(id = id),
                )
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete audio file", e)
                Result.Error("Failed to delete audio file: ${e.message}")
            }
        }
    }

    override suspend fun downloadAudioFile(
        bucketId: String,
        storagePath: String,
    ): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.functions.invoke(
                    function = FN_DOWNLOAD,
                    body = DownloadRequest(bucketId = bucketId, storagePath = storagePath),
                )
                val bytes = response.body<ByteArray>()
                Result.Success(bytes)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download audio file", e)
                Result.Error("Failed to download audio file: ${e.message}")
            }
        }
    }
}
