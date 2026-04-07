package com.waveform.ui.screen.files

import com.waveform.domain.model.AudioFileInfo

data class RemoteFilesScreenState(
    val isLoading: Boolean = false,
    val files: List<AudioFileInfo> = emptyList(),
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    /** ID of the file currently being downloaded, null if none. */
    val downloadingFileId: String? = null,
    val uploadInProgress: Boolean = false,
)
