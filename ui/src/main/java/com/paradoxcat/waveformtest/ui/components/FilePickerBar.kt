package com.paradoxcat.waveformtest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.paradoxcat.waveformtest.ui.theme.ParadoxWaveViewerTheme

@Composable
fun FilePickerBar(
    fileName: String?,
    onPickFile: () -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = onPickFile),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (fileName == null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                        contentDescription = "Pick audio file",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = fileName ?: "Tap to Pick Audio File",
                    style = if (fileName != null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
            }
        }

        if (fileName != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(onClick = onPickFile),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp, // make this button more appealing to press
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 24.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = "Change selected audio file",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun FilePickerBarPreviewNoFile() {
    ParadoxWaveViewerTheme {
        FilePickerBar(fileName = null, onPickFile = {})
    }
}

@PreviewLightDark
@Composable
fun FilePickerBarPreviewFileSelected() {
    ParadoxWaveViewerTheme {
        FilePickerBar(
            fileName = "example_long_audio_file_name_that_should_be_ellipsized.wav",
            onPickFile = {}
        )
    }
}
