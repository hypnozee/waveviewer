package com.paradoxcat.waveformtest.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paradoxcat.waveformtest.ui.components.FilePickerBar
import com.paradoxcat.waveformtest.ui.components.NormalizationToggle
import com.paradoxcat.waveformtest.ui.components.PlaybackControls
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

    val pickFileAction = {
        onEvent(WaveScreenEvent.PickFileClicked)
        filePickerLauncher.launch(arrayOf("audio/wav", "audio/x-wav"))
    }

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
            Spacer(modifier = Modifier.height(16.dp))

            FilePickerBar(
                fileName = uiState.fileName,
                onPickFile = pickFileAction
            )

            NormalizationToggle(
                dynamicNormalizationEnabled = uiState.dynamicNormalizationEnabled,
                onToggle = { onEvent(WaveScreenEvent.ToggleDynamicNormalization) }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .padding(top = 8.dp)
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

            uiState.fileUri?.let {
                PlaybackControls(
                    isPlaying = uiState.isPlaying,
                    isBuffering = uiState.isLoadingFile || uiState.isLoadingWaveform || uiState.isPlayerLoading,
                    currentPositionMillis = uiState.currentPositionMillis,
                    totalDurationMillis = uiState.totalDurationMillis,
                    onPlayPauseClicked = { onEvent(WaveScreenEvent.PlayPauseClicked) },
                    formatMillis = ::formatMillis // Pass the function reference
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

fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
