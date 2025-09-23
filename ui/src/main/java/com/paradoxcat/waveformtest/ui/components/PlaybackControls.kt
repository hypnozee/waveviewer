package com.paradoxcat.waveformtest.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paradoxcat.waveformtest.ui.theme.ParadoxWaveViewerTheme
import java.util.Locale

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMillis: Long,
    totalDurationMillis: Long,
    onPlayPauseClicked: () -> Unit,
    formatMillis: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalButton(
            onClick = onPlayPauseClicked,
            enabled = !isBuffering && totalDurationMillis > 0,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPlaying) "Pause" else "Play")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Position: ${formatMillis(currentPositionMillis)} / ${formatMillis(totalDurationMillis)}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun previewFormatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true, name = "Playback Controls - Playing")
@Composable
fun PlaybackControlsPreviewPlaying() {
    ParadoxWaveViewerTheme {
        PlaybackControls(
            isPlaying = true,
            isBuffering = false,
            currentPositionMillis = 90000, // 01:30
            totalDurationMillis = 210000, // 03:30
            onPlayPauseClicked = {},
            formatMillis = ::previewFormatMillis
        )
    }
}

@Preview(showBackground = true, name = "Playback Controls - Paused")
@Composable
fun PlaybackControlsPreviewPaused() {
    ParadoxWaveViewerTheme {
        PlaybackControls(
            isPlaying = false,
            isBuffering = false,
            currentPositionMillis = 0,
            totalDurationMillis = 180000, // 03:00
            onPlayPauseClicked = {},
            formatMillis = ::previewFormatMillis
        )
    }
}

@Preview(showBackground = true, name = "Playback Controls - Buffering")
@Composable
fun PlaybackControlsPreviewBuffering() {
    ParadoxWaveViewerTheme {
        PlaybackControls(
            isPlaying = false,
            isBuffering = true,
            currentPositionMillis = 0,
            totalDurationMillis = 180000, // 03:00
            onPlayPauseClicked = {},
            formatMillis = ::previewFormatMillis
        )
    }
}
