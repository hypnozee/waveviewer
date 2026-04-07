package com.waveform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.waveform.ui.screen.WaveScreen
import com.waveform.ui.screen.WaveScreenIntent
import com.waveform.ui.screen.WaveViewModel
import com.waveform.ui.screen.auth.AuthEvent
import com.waveform.ui.screen.auth.AuthScreen
import com.waveform.ui.screen.auth.AuthViewModel
import com.waveform.ui.screen.files.RemoteFilesEvent
import com.waveform.ui.screen.files.RemoteFilesScreen
import com.waveform.ui.screen.files.RemoteFilesViewModel
import com.waveform.ui.theme.WaveViewerTheme
import kotlinx.serialization.Serializable
import org.koin.androidx.viewmodel.ext.android.viewModel

// ── Navigation destinations ───────────────────────────────────────────────────

@Serializable
data object MainRoute : NavKey
@Serializable
data object AuthRoute : NavKey
@Serializable
data object FilesRoute : NavKey

// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    // Activity-scoped ViewModels — Koin manages lifecycle
    private val waveViewModel: WaveViewModel by viewModel()
    private val authViewModel: AuthViewModel by viewModel()
    private val filesViewModel: RemoteFilesViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaveViewerTheme {
                val backStack: NavBackStack<NavKey> = rememberNavBackStack(MainRoute)

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    entryProvider = entryProvider {

                        entry<MainRoute> {
                            WaveScreen(
                                viewState = waveViewModel.viewStateFlow.collectAsState().value,
                                onIntent = waveViewModel::processIntent,
                                onNavigateToAuth = { backStack.add(AuthRoute) },
                                onNavigateToFiles = { backStack.add(FilesRoute) },
                            )
                        }

                        entry<AuthRoute> {
                            val authState = authViewModel.state.collectAsState().value

                            LaunchedEffect(authViewModel) {
                                authViewModel.events.collect { event ->
                                    if (event is AuthEvent.Success) {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                }
                            }

                            AuthScreen(
                                state = authState,
                                onModeChanged = authViewModel::onModeChanged,
                                onEmailChanged = authViewModel::onEmailChanged,
                                onPasswordChanged = authViewModel::onPasswordChanged,
                                onConfirmPasswordChanged = authViewModel::onConfirmPasswordChanged,
                                onOtpCodeChanged = authViewModel::onOtpCodeChanged,
                                onSubmit = authViewModel::submit,
                                onVerifyOtp = authViewModel::verifyOtp,
                                onBack = { backStack.removeAt(backStack.lastIndex) },
                            )
                        }

                        entry<FilesRoute> {
                            val filesState = filesViewModel.state.collectAsState().value

                            LaunchedEffect(filesViewModel) {
                                filesViewModel.events.collect { event ->
                                    if (event is RemoteFilesEvent.FileReadyForPlayback) {
                                        waveViewModel.processIntent(
                                            WaveScreenIntent.FileSelected(event.uri)
                                        )
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                }
                            }

                            RemoteFilesScreen(
                                state = filesState,
                                onDownloadAndPlay = filesViewModel::downloadAndPlay,
                                onUpload = filesViewModel::upload,
                                onDelete = filesViewModel::deleteFile,
                                onRefresh = filesViewModel::refresh,
                                onClearError = filesViewModel::clearError,
                                onBack = { backStack.removeAt(backStack.lastIndex) },
                            )
                        }
                    }
                )
            }
        }
    }
}