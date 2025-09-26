package com.paradoxcat.waveformtest.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.paradoxcat.waveformtest.ui.theme.ParadoxWaveViewerTheme

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentAudioPosition: String,
    totalAudioDuration: String,
    onPlayPauseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPlayPauseClicked,
            enabled = !isBuffering && totalAudioDuration != "00:00",
            shape = MaterialTheme.shapes.medium,
        ) {
            PlayPauseButtonContent(isPlaying = isPlaying)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$currentAudioPosition / $totalAudioDuration",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PlayPauseButtonContent(isPlaying: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (isPlaying) "Pause" else "Play",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@PreviewLightDark
@Composable
fun PlaybackControlsPreviewPlaying() {
    ParadoxWaveViewerTheme {
        PlaybackControls(
            isPlaying = true,
            isBuffering = false,
            currentAudioPosition = "01:30",
            totalAudioDuration = "03:30",
            onPlayPauseClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
fun PlaybackControlsPaused() {
    ParadoxWaveViewerTheme {
        PlaybackControls(
            isPlaying = false,
            isBuffering = false,
            currentAudioPosition = "00:00",
            totalAudioDuration = "03:00",
            onPlayPauseClicked = {}
        )
    }
}

@PreviewLightDark
@Composable
fun PlaybackControlsPreviewBuffering() {
    ParadoxWaveViewerTheme {
        PlaybackControls(
            isPlaying = false,
            isBuffering = true,
            currentAudioPosition = "00:00",
            totalAudioDuration = "03:00",
            onPlayPauseClicked = {}
        )
    }
}
