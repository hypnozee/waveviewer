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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.paradoxcat.waveformtest.domain.model.WaveformSegment
import com.paradoxcat.waveformtest.ui.components.FilePickerBar
import com.paradoxcat.waveformtest.ui.components.NormalizationToggle
import com.paradoxcat.waveformtest.ui.components.NumSegmentsControl
import com.paradoxcat.waveformtest.ui.components.PlaybackControls
import com.paradoxcat.waveformtest.ui.components.WaveformChart
import com.paradoxcat.waveformtest.ui.theme.ParadoxWaveViewerTheme
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Allows picking an audio file (WAV format),
 * visualizing its waveform,
 * and controlling basic playback (play/pause).
 */
@Composable
fun WaveScreen(
    viewState: WaveScreenState,
    onIntent: (WaveScreenIntent) -> Unit,
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                onIntent(WaveScreenIntent.FileSelected(it))
            }
        }
    )

    val pickFileAction = {
        onIntent(WaveScreenIntent.PickFileClicked)
        filePickerLauncher.launch(arrayOf("audio/wav", "audio/x-wav"))
    }

    viewState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            delay(5000L)
            onIntent(WaveScreenIntent.ClearErrorMessage)
        }
    }

    WaveScreenContent(
        viewState = viewState,
        onIntent = onIntent,
        pickFileAction = pickFileAction
    )
}

@Composable
fun WaveScreenContent(
    viewState: WaveScreenState,
    onIntent: (WaveScreenIntent) -> Unit,
    pickFileAction: () -> Unit,
) {

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Text(text = "Waveform Visualizer", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            NormalizationToggle(
                dynamicNormalizationEnabled = viewState.dynamicNormalizationEnabled,
                onToggle = { onIntent(WaveScreenIntent.ToggleDynamicNormalization) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilePickerBar(
                fileName = viewState.fileName,
                onPickFile = pickFileAction
            )

            Spacer(modifier = Modifier.height(8.dp))

            NumSegmentsControl(
                currentNumSegments = viewState.currentNumSegments,
                onIntent = onIntent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    if (viewState.waveformData != null && viewState.waveformData.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            val currentPositionFraction = if (viewState.totalDurationMillis > 0) {
                                viewState.currentPositionMillis.toFloat() / viewState.totalDurationMillis.toFloat()
                            } else {
                                0f
                            }
                            Text(
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                                text = String.format(
                                    Locale.US,
                                    "%.2f",
                                    viewState.displayMaxAmplitude
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                            WaveformChart(
                                waveformData = viewState.waveformData,
                                playProgress = currentPositionFraction,
                                dynamicNormalizationEnabled = viewState.dynamicNormalizationEnabled,
                                yMin = viewState.displayMinAmplitude,
                                yMax = viewState.displayMaxAmplitude,
                                modifier = Modifier
                                    .weight(1f) // Chart takes available vertical space
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                onSeekIntent = { fraction ->
                                    onIntent(WaveScreenIntent.SeekTo(fraction))
                                },
                            )
                            Text(
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                text = String.format(
                                    Locale.US,
                                    "%.2f",
                                    viewState.displayMinAmplitude
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (viewState.fileUri == null && !viewState.isLoadingFile && !viewState.isLoadingWaveform) {
                                Text(
                                    "Select a WAV file to see the waveform.",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            } else if (viewState.waveformData?.isEmpty() == true && viewState.fileUri != null && !viewState.isLoadingWaveform && !viewState.isLoadingFile) {
                                Text(
                                    "Waveform data is empty (file might be silent or too short).",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            } else if (viewState.waveformData == null && viewState.fileUri != null && !viewState.isLoadingFile && !viewState.isLoadingWaveform) {
                                Text(
                                    "No waveform data to display (select a file).",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                if (viewState.isLoadingFile || viewState.isLoadingWaveform) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (viewState.isLoadingFile) "Loading file..." else "Processing waveform...")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            viewState.fileUri?.let {
                PlaybackControls(
                    isPlaying = viewState.isPlaying,
                    isBuffering = viewState.isLoadingFile || viewState.isLoadingWaveform || viewState.isPlayerLoading,
                    currentAudioPosition = viewState.currentAudioPosition,
                    totalAudioDuration = viewState.totalAudioDuration,
                    onPlayPauseClicked = { onIntent(WaveScreenIntent.PlayPauseClicked) },
                )
            }

            viewState.errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { onIntent(WaveScreenIntent.ClearErrorMessage) }) { Text("Dismiss") }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun WaveScreenPreviewChart() {
    ParadoxWaveViewerTheme {
        val waveformData = listOf(
            WaveformSegment(0.0f, 0.5f),
            WaveformSegment(-0.2f, 0.3f),
            WaveformSegment(0.1f, 0.8f),
            WaveformSegment(-0.5f, 0.0f),
        )
        WaveScreenContent(
            viewState = WaveScreenState(
                fileName = "haha.wav",
                waveformData = waveformData,
                displayMinAmplitude = -1.0f,
                displayMaxAmplitude = 1.0f
            ),
            onIntent = {},
            pickFileAction = {}
        )
    }
}
