package com.waveform.ui.screen

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.waveform.domain.model.AuthState
import com.waveform.domain.model.WaveformSegment
import com.waveform.ui.components.FilePickerBar
import com.waveform.ui.components.NormalizationToggle
import com.waveform.ui.components.NumSegmentsControl
import com.waveform.ui.components.PlaybackControls
import com.waveform.ui.components.WaveformChart
import com.waveform.ui.theme.WaveViewerTheme
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun WaveScreen(
    viewState: WaveScreenState,
    onIntent: (WaveScreenIntent) -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToFiles: () -> Unit,
) {
    var showFileSourceDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { onIntent(WaveScreenIntent.FileSelected(it)) }
        }
    )

    val pickFromDevice = {
        onIntent(WaveScreenIntent.PickFileClicked)
        filePickerLauncher.launch(arrayOf("audio/wav", "audio/x-wav"))
    }

    viewState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            delay(5000L)
            onIntent(WaveScreenIntent.ClearErrorMessage)
        }
    }

    if (showFileSourceDialog) {
        FileSourceDialog(
            isAuthenticated = viewState.authState is AuthState.Authenticated,
            onDismiss = { showFileSourceDialog = false },
            onLoginSignUp = { showFileSourceDialog = false; onNavigateToAuth() },
            onBrowseFiles = { showFileSourceDialog = false; onNavigateToFiles() },
            onPickFromDevice = { showFileSourceDialog = false; pickFromDevice() },
            onSignOut = { showFileSourceDialog = false; onIntent(WaveScreenIntent.SignOutClicked) },
        )
    }

    WaveScreenContent(
        viewState = viewState,
        onIntent = onIntent,
        onFilePickerBarClicked = { showFileSourceDialog = true },
    )
}

@Composable
private fun FileSourceDialog(
    isAuthenticated: Boolean,
    onDismiss: () -> Unit,
    onLoginSignUp: () -> Unit,
    onBrowseFiles: () -> Unit,
    onPickFromDevice: () -> Unit,
    onSignOut: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Audio File") },
        text = {
            Column {
                if (isAuthenticated) {
                    TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign Out")
                    }
                    TextButton(onClick = onBrowseFiles, modifier = Modifier.fillMaxWidth()) {
                        Text("Browse My Files")
                    }
                } else {
                    TextButton(onClick = onLoginSignUp, modifier = Modifier.fillMaxWidth()) {
                        Text("Login / Sign Up")
                    }
                    TextButton(onClick = onBrowseFiles, modifier = Modifier.fillMaxWidth()) {
                        Text("Browse Public Files")
                    }
                }
                TextButton(onClick = onPickFromDevice, modifier = Modifier.fillMaxWidth()) {
                    Text("Pick from Device")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WaveScreenContent(
    viewState: WaveScreenState,
    onIntent: (WaveScreenIntent) -> Unit,
    onFilePickerBarClicked: () -> Unit,
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
                onPickFile = onFilePickerBarClicked,
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
                    if (!viewState.waveformData.isNullOrEmpty()) {
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
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                onDragStarted = { onIntent(WaveScreenIntent.SeekDragStarted) },
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
                    isBuffering = viewState.isLoadingFile || viewState.isLoadingWaveform || viewState.isPlayerLoading || viewState.isSeeking,
                    currentPositionMillis = viewState.currentPositionMillis,
                    totalDurationMillis = viewState.totalDurationMillis,
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
    WaveViewerTheme {
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
            onFilePickerBarClicked = {},
        )
    }
}
