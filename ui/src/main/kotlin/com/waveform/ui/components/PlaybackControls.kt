package com.waveform.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.waveform.ui.theme.WaveViewerTheme
import java.util.Locale

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMillis: Long,
    totalDurationMillis: Long,
    onPlayPauseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPlayPauseClicked,
            enabled = !isBuffering && totalDurationMillis > 0,
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${formatMillis(currentPositionMillis)} / ${formatMillis(totalDurationMillis)}",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

private fun formatMillis(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@PreviewLightDark
@Composable
fun PlaybackControlsPaused() {
    WaveViewerTheme {
        PlaybackControls(
            isPlaying = false,
            isBuffering = false,
            currentPositionMillis = 0L,
            totalDurationMillis = 180_000L,
            onPlayPauseClicked = {}
        )
    }
}
