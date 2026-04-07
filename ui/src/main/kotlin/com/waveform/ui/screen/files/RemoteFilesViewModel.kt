package com.waveform.ui.screen.files

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveform.domain.core.Result
import com.waveform.domain.model.AudioFileInfo
import com.waveform.domain.model.AuthState
import com.waveform.domain.usecase.DeleteAudioFileUseCase
import com.waveform.domain.usecase.DownloadAudioFileUseCase
import com.waveform.domain.usecase.GetPublicAudioFilesUseCase
import com.waveform.domain.usecase.GetUserAudioFilesUseCase
import com.waveform.domain.usecase.ObserveAuthStateUseCase
import com.waveform.domain.usecase.UploadAudioFileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class RemoteFilesViewModel(
    private val app: Application,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getPublicAudioFilesUseCase: GetPublicAudioFilesUseCase,
    private val getUserAudioFilesUseCase: GetUserAudioFilesUseCase,
    private val downloadAudioFileUseCase: DownloadAudioFileUseCase,
    private val uploadAudioFileUseCase: UploadAudioFileUseCase,
    private val deleteAudioFileUseCase: DeleteAudioFileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RemoteFilesScreenState())
    val state = _state.asStateFlow()

    private val _events = Channel<RemoteFilesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase()
                .filter { it !is AuthState.Loading }
                .distinctUntilChanged()
                .collect { authState ->
                    val isAuthenticated = authState is AuthState.Authenticated
                    _state.update { it.copy(isAuthenticated = isAuthenticated) }
                    loadFiles(isAuthenticated)
                }
        }
    }

    fun refresh() {
        loadFiles(_state.value.isAuthenticated)
    }

    fun downloadAndPlay(file: AudioFileInfo) {
        if (_state.value.downloadingFileId != null) return
        _state.update { it.copy(downloadingFileId = file.id, errorMessage = null) }
        viewModelScope.launch {
            when (val result = downloadAudioFileUseCase(file.bucketId, file.storagePath)) {
                is Result.Success -> {
                    val uri = saveToCache(file.name, result.data)
                    _state.update { it.copy(downloadingFileId = null) }
                    _events.send(RemoteFilesEvent.FileReadyForPlayback(uri))
                }
                is Result.Error -> {
                    _state.update { it.copy(downloadingFileId = null, errorMessage = result.message) }
                }
            }
        }
    }

    fun upload(uri: Uri) {
        _state.update { it.copy(uploadInProgress = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val fileName = resolveFileName(uri)
                val mimeType = app.contentResolver.getType(uri) ?: "audio/wav"
                val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    _state.update { it.copy(uploadInProgress = false, errorMessage = "Could not read file.") }
                    return@launch
                }
                when (val result = uploadAudioFileUseCase(fileName, mimeType, bytes)) {
                    is Result.Success -> {
                        _state.update { it.copy(uploadInProgress = false) }
                        loadFiles(isAuthenticated = true)
                    }
                    is Result.Error -> {
                        _state.update { it.copy(uploadInProgress = false, errorMessage = result.message) }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(uploadInProgress = false, errorMessage = "Upload failed: ${e.message}") }
            }
        }
    }

    fun deleteFile(file: AudioFileInfo) {
        viewModelScope.launch {
            when (val result = deleteAudioFileUseCase(file.id)) {
                is Result.Success -> loadFiles(_state.value.isAuthenticated)
                is Result.Error -> _state.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun loadFiles(isAuthenticated: Boolean) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (isAuthenticated) getUserAudioFilesUseCase() else getPublicAudioFilesUseCase()
            when (result) {
                is Result.Success -> _state.update { it.copy(isLoading = false, files = result.data) }
                is Result.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    private fun saveToCache(fileName: String, bytes: ByteArray): Uri {
        val cacheFile = File(app.cacheDir, "downloaded_$fileName")
        cacheFile.writeBytes(bytes)
        return Uri.fromFile(cacheFile)
    }

    private fun resolveFileName(uri: Uri): String {
        return app.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: "audio.wav"
    }
}

sealed interface RemoteFilesEvent {
    data class FileReadyForPlayback(val uri: Uri) : RemoteFilesEvent
}
