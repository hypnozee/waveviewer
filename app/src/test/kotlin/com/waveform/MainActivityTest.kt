package com.waveform

import android.app.Application
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.waveform.ui.screen.WaveScreenIntent
import com.waveform.ui.screen.WaveScreenState
import com.waveform.ui.screen.WaveViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

// Custom Application for testing to prevent MainApplication from starting real Koin
class TestApplication : Application()

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O], application = TestApplication::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel: WaveViewModel = mock()
    private val viewStateFlow = MutableStateFlow(WaveScreenState())

    @Before
    fun setup() {
        ShadowLog.stream = System.out

        // Ensure Koin is stopped before starting a new one (just in case)
        stopKoin()

        // Mock the ViewModel flow
        whenever(mockViewModel.viewStateFlow).doReturn(viewStateFlow)

        // Start Koin with a test module injecting the mock ViewModel
        startKoin {
            modules(module {
                viewModel { mockViewModel }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Content view set successfully`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Verify that setContent is called and the underlying Compose view hierarchy 
            // is attached to the Activity's window.
            composeTestRule.onNodeWithText("Waveform Visualizer").assertIsDisplayed()
        }
    }

    @Test
    fun `UI structure verification`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Confirm that the WaveScreen composable is present in the UI hierarchy 
            // after the activity launches.
            composeTestRule.onNodeWithText("Waveform Visualizer").assertIsDisplayed()
            composeTestRule.onNodeWithText("True Amplitude").assertIsDisplayed()
            composeTestRule.onNodeWithText("Dynamic Height").assertIsDisplayed()
        }
    }

    @Test
    fun `ViewModel interaction wiring`() {
        // Verify that the WaveScreen is correctly observing the viewModel's viewStateFlow 
        // and that interactions on the screen trigger the viewModel's processIntent method.
        val testFileName = "test_audio.wav"
        viewStateFlow.value = WaveScreenState(fileName = testFileName)

        ActivityScenario.launch(MainActivity::class.java).use {
            // Verify UI shows data from ViewModel
            composeTestRule.onNodeWithText(testFileName).assertIsDisplayed()

            // Perform interaction
            composeTestRule.onNodeWithText("Dynamic Height").performClick()

            // Verify ViewModel receives intent
            verify(mockViewModel).processIntent(WaveScreenIntent.ToggleDynamicNormalization)
        }
    }

    @Test
    fun `Recreation with SavedInstanceState`() {
        // Test that the Activity can be recreated (e.g., after configuration change or process death) 
        // with a non-null savedInstanceState without crashing.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeTestRule.onNodeWithText("Waveform Visualizer").assertIsDisplayed()

            // Recreate activity
            scenario.recreate()

            // Verify it still works and doesn't crash
            composeTestRule.onNodeWithText("Waveform Visualizer").assertIsDisplayed()
        }
    }

    @Test
    fun `Theme application`() {
        // Check if the WaveViewerTheme is applied correctly to the content, 
        // ensuring design system tokens (colors, typography) are accessible in the hierarchy.
        ActivityScenario.launch(MainActivity::class.java).use {
            // Ensuring the activity launches and content is displayed implies theme didn't crash it.
            // Precise visual verification would require screenshot testing.
            composeTestRule.onNodeWithText("Waveform Visualizer").assertIsDisplayed()
        }
    }

    @Test
    fun `Dependency Injection failure handling`() {
        // Edge case: Verify behavior or crash reporting if the Koin module is not loaded 
        // and the viewModel fails to inject during onCreate.
        stopKoin() // Stop Koin to simulate missing DI

        try {
            ActivityScenario.launch(MainActivity::class.java)
            fail("Activity launch should fail without Koin initialization")
        } catch (_: Exception) {
            // Expected failure.
        }
    }

    @Test
    fun `Lifecycle state consistency`() {
        // Ensure the activity transitions to the CREATED state successfully 
        // after onCreate finishes execution.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            assertEquals(Lifecycle.State.CREATED, scenario.state)

            scenario.moveToState(Lifecycle.State.RESUMED)
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
