package com.paradoxcat.waveformtest.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.paradoxcat.waveformtest.ui.screen.MAX_NUM_SEGMENTS
import com.paradoxcat.waveformtest.ui.screen.MIN_NUM_SEGMENTS
import com.paradoxcat.waveformtest.ui.screen.WaveScreenIntent
import com.paradoxcat.waveformtest.ui.theme.ParadoxWaveViewerTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumSegmentsControl(
    currentNumSegments: Int,
    onIntent: (WaveScreenIntent) -> Unit,
) {
    val view = LocalView.current // needed for vibration
    val interactionSource = remember { MutableInteractionSource() }
    var sliderPosition by remember { mutableFloatStateOf(currentNumSegments.toFloat()) }
    var currentStepHaptics by remember { mutableIntStateOf(currentNumSegments) }
    var isDragging by remember { mutableStateOf(false) } // when dragging, we increase text size so we don't need glasses

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) { // so slider only knows when drag is happening or stopped, couldn't make it intercept thumb press
                is DragInteraction.Start -> isDragging = true
                is DragInteraction.Stop -> isDragging = false
            }
        }
    }

    LaunchedEffect(currentNumSegments) {
        val newNumSegments = currentNumSegments.toFloat()
        if (sliderPosition != newNumSegments) {
            sliderPosition = newNumSegments
            currentStepHaptics = newNumSegments.roundToInt()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalArrangement = Arrangement.Center
        ) {
            val sliderColors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                activeTickColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f),
                inactiveTickColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.25f)
            )

            Slider(
                value = sliderPosition,
                onValueChange = { newValue ->
                    sliderPosition = newValue
                    val currentStep = newValue.roundToInt()
                    if (currentStepHaptics != currentStep) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        currentStepHaptics = currentStep
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                steps = (MAX_NUM_SEGMENTS - MIN_NUM_SEGMENTS) / 50 - 1,
                valueRange = MIN_NUM_SEGMENTS.toFloat()..MAX_NUM_SEGMENTS.toFloat(),
                onValueChangeFinished = {
                    onIntent(WaveScreenIntent.NumSegmentsChanged(sliderPosition.roundToInt()))
                },
                interactionSource = interactionSource,
                colors = sliderColors,
                track = { currentTrackSliderState: SliderState ->
                    SliderDefaults.Track(
                        sliderState = currentTrackSliderState,
                        colors = sliderColors,
                        thumbTrackGapSize = 0.dp,
                    )
                },
                thumb = {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            thumbSize = DpSize(8.dp, 36.dp),
                        )
                        Text(
                            text = sliderPosition.roundToInt().toString(),
                            style = if (isDragging) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.graphicsLayer {
                                translationY = -(26.dp.toPx())
                            }
                        )
                    }
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
fun NumSegmentsControlPreview() {
    ParadoxWaveViewerTheme {
        NumSegmentsControl(
            currentNumSegments = 650,
            onIntent = {}
        )
    }
}

