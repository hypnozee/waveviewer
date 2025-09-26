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
import com.paradoxcat.waveformtest.ui.screen.MAX_TARGET_SEGMENTS
import com.paradoxcat.waveformtest.ui.screen.MIN_TARGET_SEGMENTS
import com.paradoxcat.waveformtest.ui.screen.WaveScreenIntent
import com.paradoxcat.waveformtest.ui.theme.ParadoxWaveViewerTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetSegmentsControl(
    currentTargetSegments: Int,
    onIntent: (WaveScreenIntent) -> Unit,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    var sliderPosition by remember { mutableFloatStateOf(currentTargetSegments.toFloat()) }
    var previousRoundedValueForHaptics by remember { mutableIntStateOf(currentTargetSegments) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> isDragging = true
                is DragInteraction.Stop -> isDragging = false
            }
        }
    }

    LaunchedEffect(currentTargetSegments) {
        val newTargetFloat = currentTargetSegments.toFloat()
        if (sliderPosition != newTargetFloat) {
            sliderPosition = newTargetFloat
            previousRoundedValueForHaptics = newTargetFloat.roundToInt()
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
                inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                activeTickColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f),
                inactiveTickColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.24f)
            )

            Slider(
                value = sliderPosition,
                onValueChange = { newValue ->
                    sliderPosition = newValue
                    val currentRoundedValue = newValue.roundToInt()
                    if (previousRoundedValueForHaptics != currentRoundedValue) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        previousRoundedValueForHaptics = currentRoundedValue
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                steps = (MAX_TARGET_SEGMENTS - MIN_TARGET_SEGMENTS) / 50 - 1,
                valueRange = MIN_TARGET_SEGMENTS.toFloat()..MAX_TARGET_SEGMENTS.toFloat(),
                onValueChangeFinished = {
                    onIntent(WaveScreenIntent.TargetSegmentsChanged(sliderPosition.roundToInt()))
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
                    CustomSliderThumb(
                        sliderPosition = sliderPosition,
                        interactionSource = interactionSource,
                        isDragging = isDragging
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomSliderThumb(
    sliderPosition: Float,
    interactionSource: MutableInteractionSource,
    isDragging: Boolean,
) {
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

@PreviewLightDark
@Composable
fun TargetSegmentsControlPreview() {
    ParadoxWaveViewerTheme {
        TargetSegmentsControl(
            currentTargetSegments = 650,
            onIntent = {}
        )
    }
}

