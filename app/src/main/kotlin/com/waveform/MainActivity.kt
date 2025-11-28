package com.waveform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import com.waveform.ui.screen.WaveScreen
import com.waveform.ui.screen.WaveViewModel
import com.waveform.ui.theme.WaveViewerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * The only Activity in the app.
 *
 * Compose for UI, Koin for DI.
 * [WaveScreen] is the main content.
 */
class MainActivity : ComponentActivity() {

    // VM injection
    private val viewModel: WaveViewModel by viewModel()

    /**
     * Setup the content view of the activity.
     * Using a nice [WaveViewerTheme] displaying [WaveScreen] controlled by [viewModel]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaveViewerTheme {
                WaveScreen(
                    viewState = viewModel.viewStateFlow.collectAsState().value,
                    onIntent = viewModel::processIntent,
                )
            }
        }
    }
}