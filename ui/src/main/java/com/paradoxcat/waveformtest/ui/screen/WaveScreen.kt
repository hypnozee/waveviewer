package com.paradoxcat.waveformtest.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paradoxcat.waveformtest.domain.model.WaveformSegment
import com.paradoxcat.waveformtest.ui.components.WaveformChart
import java.util.Locale

/**
 * Allows picking an audio file (WAV format),
 * visualizing its waveform,
 * and controlling basic playback (play/pause).
 */
@Composable
fun WaveScreen(
    viewModel: WaveViewModel,
    onEvent: (WaveScreenEvent) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                onEvent(WaveScreenEvent.FileSelected(it))
            }
        }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Waveform Visualizer", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))

            // Pick file
            FilledTonalButton(
                onClick = {
                    onEvent(WaveScreenEvent.PickFileClicked)
                    filePickerLauncher.launch(arrayOf("audio/wav", "audio/x-wav"))
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Pick Audio File")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Select view mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                if (uiState.dynamicNormalizationEnabled) {
                                    onEvent(WaveScreenEvent.ToggleDynamicNormalization)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = !uiState.dynamicNormalizationEnabled,
                            onClick = {
                                if (uiState.dynamicNormalizationEnabled) {
                                    onEvent(WaveScreenEvent.ToggleDynamicNormalization)
                                }
                            }
                        )
                        Text("True Amplitude", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    // "Fill Height" (Dynamic Normalization) option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                if (!uiState.dynamicNormalizationEnabled) {
                                    onEvent(WaveScreenEvent.ToggleDynamicNormalization)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = uiState.dynamicNormalizationEnabled,
                            onClick = {
                                if (!uiState.dynamicNormalizationEnabled) {
                                    onEvent(WaveScreenEvent.ToggleDynamicNormalization)
                                }
                            }
                        )
                        Text("Fill Height", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Display selected file name.
            uiState.fileName?.let {
                Text("File: $it", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (uiState.waveformData != null) {
                            val currentPositionFraction = if (uiState.totalDurationMillis > 0) {
                                uiState.currentPositionMillis.toFloat() / uiState.totalDurationMillis.toFloat()
                            } else {
                                0f
                            }
                            if (uiState.waveformData!!.isNotEmpty()) {
                                WaveformChart(
                                    waveformData = uiState.waveformData!!,
                                    currentPositionFraction = currentPositionFraction,
                                    dynamicNormalizationEnabled = uiState.dynamicNormalizationEnabled,
                                    modifier = Modifier.fillMaxSize(),
                                    onEvent = onEvent,
                                )
                            } else if (uiState.fileUri != null && !uiState.isLoadingWaveform && !uiState.isLoadingFile) {
                                Text(
                                    "Waveform data is empty (file might be silent or too short).",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else if (uiState.fileUri != null && !uiState.isLoadingFile && !uiState.isLoadingWaveform) {
                            Text(
                                "No waveform data to display (select a file).",
                                style = MaterialTheme.typography.labelMedium
                            )
                        } else if (!uiState.isLoadingFile && !uiState.isLoadingWaveform) {
                            Text(
                                "Select a WAV file to see the waveform.",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (uiState.isLoadingFile || uiState.isLoadingWaveform) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (uiState.isLoadingFile) "Loading file..." else "Processing waveform...")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Play/Pause
            uiState.fileUri?.let {
                FilledTonalButton(
                    onClick = { onEvent(WaveScreenEvent.PlayPauseClicked) },
                    enabled = !uiState.isLoadingFile && !uiState.isLoadingWaveform && !uiState.isPlayerLoading,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isPlaying) "Pause" else "Play")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Position: ${formatMillis(uiState.currentPositionMillis)} / ${
                        formatMillis(
                            uiState.totalDurationMillis
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            uiState.errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WaveScreenPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text("Waveform Visualizer (Preview)", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(
                onClick = {},
                shape = MaterialTheme.shapes.medium
            ) { Text("Pick Audio File") }
            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        RadioButton(selected = true, onClick = {})
                        Text("True Amplitude", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        RadioButton(selected = false, onClick = {})
                        Text("Fill Height", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text("File: example.wav", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val previewWaveformData = listOf(
                            WaveformSegment(-0.5f, 0.5f), WaveformSegment(-0.2f, 0.8f),
                            WaveformSegment(-0.8f, 0.2f), WaveformSegment(-0.3f, 0.3f),
                            WaveformSegment(0.1f, 0.9f), WaveformSegment(-0.6f, 0.6f)
                        )
                        WaveformChart(
                            waveformData = previewWaveformData,
                            currentPositionFraction = 0.3f,
                            dynamicNormalizationEnabled = false,
                            modifier = Modifier.fillMaxSize(),
                            onEvent = {}
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(
                onClick = {},
                shape = MaterialTheme.shapes.medium
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Position: 00:15 / 03:30",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
