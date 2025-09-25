package com.paradoxcat.waveformtest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.paradoxcat.waveformtest.ui.screen.MAX_TARGET_SEGMENTS
import com.paradoxcat.waveformtest.ui.screen.MIN_TARGET_SEGMENTS
import com.paradoxcat.waveformtest.ui.screen.WaveScreenIntent
import kotlin.math.roundToInt

@Composable
fun TargetSegmentsControl(
    currentTargetSegments: Int,
    onIntent: (WaveScreenIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Local state for the slider's raw float position.
        var sliderPosition by remember { mutableFloatStateOf(currentTargetSegments.toFloat()) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RESOLUTION:",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                // Text reads the rounded value of the local sliderPosition for live updates
                text = sliderPosition.roundToInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End
            )
        }

        // Sync slider's position if currentTargetSegments changes
        LaunchedEffect(currentTargetSegments) {
            sliderPosition = currentTargetSegments.toFloat()
        }

        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
            },
            valueRange = MIN_TARGET_SEGMENTS.toFloat()..MAX_TARGET_SEGMENTS.toFloat(),
            steps = (MAX_TARGET_SEGMENTS - MIN_TARGET_SEGMENTS) / 25 - 1,
            onValueChangeFinished = {
                // Send the intent with the final sliderPosition
                onIntent(WaveScreenIntent.TargetSegmentsChanged(sliderPosition.roundToInt()))
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
