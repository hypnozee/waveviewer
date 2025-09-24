package com.paradoxcat.waveformtest.ui.screen

import android.content.res.Configuration
import androidx.core.net.toUri
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.paradoxcat.waveformtest.domain.model.WaveformSegment
import com.paradoxcat.waveformtest.ui.theme.ParadoxWaveViewerTheme

@Preview(name = "Light Theme", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class PreviewLightDark

class WaveScreenStateProvider : PreviewParameterProvider<WaveScreenState> {
    override val values = sequenceOf(
        WaveScreenState( // No Audio Selected
            fileName = "No file selected",
            isLoadingFile = false,
            isLoadingWaveform = false,
            waveformData = null
        ),
        WaveScreenState( // Loading File State
            fileName = "sample.wav",
            isLoadingFile = true,
            isLoadingWaveform = false,
            waveformData = null
        ),
        WaveScreenState( // Processing State
            fileUri = "file:///android_asset/sample.wav".toUri(),
            fileName = "sample.wav",
            isLoadingFile = false,
            isLoadingWaveform = true,
            waveformData = null
        ),
        WaveScreenState( // Data Loaded State
            fileUri = "file:///android_asset/sample.wav".toUri(),
            fileName = "sample.wav",
            waveformData = sampleWaveformData,
            totalDurationMillis = 5000L,
            currentPositionMillis = 1000L,
            isPlaying = true,
            dynamicNormalizationEnabled = false
        ),
        WaveScreenState( // Data Loaded State - With Dynamic Normalization
            fileUri = "file:///android_asset/sample.wav".toUri(),
            fileName = "sample.wav",
            waveformData = sampleWaveformData,
            totalDurationMillis = 5000L,
            currentPositionMillis = 2500L,
            isPlaying = false,
            dynamicNormalizationEnabled = true
        ),
        WaveScreenState( // Short/Empty Waveform Data but file loaded
            fileUri = "file:///android_asset/short_or_silent.wav".toUri(),
            fileName = "short_or_silent.wav",
            waveformData = sampleWaveformDataEmpty,
            totalDurationMillis = 500L,
            currentPositionMillis = 0L,
            isPlaying = false
        ),
        WaveScreenState( // No Waveform Data
            fileUri = "file:///android_asset/somefile.wav".toUri(),
            fileName = "somefile.wav",
            waveformData = null,
            totalDurationMillis = 0L,
            currentPositionMillis = 0L,
            isPlaying = false
        ),
        WaveScreenState( // Error State
            fileName = "error_loading.wav",
            errorMessage = "Failed to load WAV file. Invalid format."
        ),
        WaveScreenState( // Error with file loaded and playback controls
            fileUri = "file:///android_asset/error_playback.wav".toUri(),
            fileName = "error_playback.wav",
            waveformData = sampleWaveformDataShort,
            totalDurationMillis = 1000L,
            errorMessage = "Playback error occurred."
        )
    )
}

@PreviewLightDark
@Composable
fun WaveScreenVariousStatesPreview(
    @PreviewParameter(WaveScreenStateProvider::class) uiState: WaveScreenState,
) {
    ParadoxWaveViewerTheme {
        WaveScreenContent(
            uiState = uiState,
            onEvent = {},
            pickFileAction = {}
        )
    }
}

@PreviewLightDark
@Composable
fun WaveScreenPreviewInitial() {
    ParadoxWaveViewerTheme {
        WaveScreenContent(
            uiState = WaveScreenState(fileName = "No file selected"),
            onEvent = {},
            pickFileAction = {}
        )
    }
}

@PreviewLightDark
@Composable
fun WaveScreenPreviewLoadingFile() {
    ParadoxWaveViewerTheme {
        WaveScreenContent(
            uiState = WaveScreenState(isLoadingFile = true, fileName = "sample.wav"),
            onEvent = {},
            pickFileAction = {}
        )
    }
}

@PreviewLightDark
@Composable
fun WaveScreenPreviewProcessingWaveform() {
    ParadoxWaveViewerTheme {
        WaveScreenContent(
            uiState = WaveScreenState(
                fileUri = "file:///android_asset/sample.wav".toUri(),
                fileName = "sample.wav",
                isLoadingWaveform = true
            ),
            onEvent = {},
            pickFileAction = {}
        )
    }
}

@PreviewLightDark
@Composable
fun WaveScreenPreviewDataLoadedPlaying() {
    ParadoxWaveViewerTheme {
        WaveScreenContent(
            uiState = WaveScreenState(
                fileUri = "file:///android_asset/sample.wav".toUri(),
                fileName = "sample.wav",
                waveformData = sampleWaveformData,
                isPlaying = true,
                currentPositionMillis = 1500,
                totalDurationMillis = 60000 // 1 minute
            ),
            onEvent = {},
            pickFileAction = {}
        )
    }
}

@PreviewLightDark
@Composable
fun WaveScreenPreviewDataLoadedPausedDynamicNorm() {
    ParadoxWaveViewerTheme {
        WaveScreenContent(
            uiState = WaveScreenState(
                fileUri = "file:///android_asset/sample.wav".toUri(),
                fileName = "sample.wav",
                waveformData = sampleWaveformData,
                isPlaying = false,
                dynamicNormalizationEnabled = true,
                currentPositionMillis = 30000, // 30 seconds
                totalDurationMillis = 120000 // 2 minutes
            ),
            onEvent = {},
            pickFileAction = {}
        )
    }
}

@PreviewLightDark
@Composable
fun WaveScreenPreviewError() {
    ParadoxWaveViewerTheme {
        WaveScreenContent(
            uiState = WaveScreenState(
                fileName = "problematic_file.wav",
                errorMessage = "This is a long and detailed error message to check how it wraps and is displayed on the screen."
            ),
            onEvent = {},
            pickFileAction = {}
        )
    }
}

private val sampleWaveformData = List(100) { i ->
    val fraction = i / 100f
    val value = kotlin.math.sin(fraction * 2 * kotlin.math.PI * 5) //
    WaveformSegment(min = -value.toFloat(), max = value.toFloat())
}
private val sampleWaveformDataShort = List(10) { i ->
    val fraction = i / 10f
    val value = kotlin.math.sin(fraction * 2 * kotlin.math.PI)
    WaveformSegment(min = -value.toFloat(), max = value.toFloat())
}
private val sampleWaveformDataEmpty = emptyList<WaveformSegment>()
