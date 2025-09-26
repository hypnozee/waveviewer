package com.paradoxcat.waveformtest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paradoxcat.waveformtest.domain.model.WaveformSegment
import kotlin.math.abs

private const val MIN_AMPLITUDE_RANGE = 0.00001f

/**
 * Chart to see a series of connected points.
 * Has a vertical line to indicate current play position.
 * You can drag the line to change the play position.
 * Has two view modes:
 * 1. True Amplitude: displays segments based on their value inside [-1.0, +1.0]
 * 2. Dynamic Normalization: scale the average amplitude of segments to fill the height.
 */
@Composable
fun WaveformChart(
    waveformData: List<WaveformSegment>,
    currentPositionFraction: Float,
    dynamicNormalizationEnabled: Boolean,
    modifier: Modifier = Modifier,
    onSeekIntent: (Float) -> Unit,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    pointColor: Color = MaterialTheme.colorScheme.secondary,
    progressLineColor: Color = MaterialTheme.colorScheme.tertiary,
    strokeWidthDp: Dp = 1.dp,
    pointRadiusDp: Dp = 2.dp,
    progressLineWidthDp: Dp = 3.dp,
) {
    val pointRadiusPx = with(LocalDensity.current) { pointRadiusDp.toPx() }
    val strokeWidthPx = with(LocalDensity.current) { strokeWidthDp.toPx() }
    val progressLineWidthPx = with(LocalDensity.current) { progressLineWidthDp.toPx() }

    val (overallMinAvg, overallMaxAvg) = remember(waveformData, dynamicNormalizationEnabled) {
        calculateAmplitudeRange(waveformData, dynamicNormalizationEnabled)
    }

    var amplitudeDisplayRange = overallMaxAvg - overallMinAvg
    if (amplitudeDisplayRange < MIN_AMPLITUDE_RANGE) { // Avoid division by zero
        amplitudeDisplayRange = 0f
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (size.width > 0) {
                            val positionFraction =
                                (offset.x / size.width).coerceIn(0f, 1f)
                            onSeekIntent(positionFraction)
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        if (size.width > 0) {
                            val positionFraction =
                                (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeekIntent(positionFraction)
                            change.consume()
                        }
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        drawWaveform(
            waveformData = waveformData,
            dynamicNormalizationEnabled = dynamicNormalizationEnabled,
            overallMinAvg = overallMinAvg,
            amplitudeDisplayRange = amplitudeDisplayRange,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            pointColor = pointColor,
            pointRadiusPx = pointRadiusPx,
            lineColor = lineColor,
            strokeWidthPx = strokeWidthPx
        )

        drawProgressLine(
            canvasWidth,
            canvasHeight,
            progressLineColor,
            currentPositionFraction,
            progressLineWidthPx
        )
    }
}

private fun DrawScope.drawWaveform(
    waveformData: List<WaveformSegment>,
    dynamicNormalizationEnabled: Boolean,
    overallMinAvg: Float,
    amplitudeDisplayRange: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    pointColor: Color,
    pointRadiusPx: Float,
    lineColor: Color,
    strokeWidthPx: Float,
) {
    val numSegments = waveformData.size
    val segmentWidth = if (numSegments > 1) canvasWidth / (numSegments - 1) else canvasWidth
    val drawingMiddleY = canvasHeight / 2f
    val maxAmplitudePx = canvasHeight / 2f

    var prevX: Float? = null
    var prevY: Float? = null

    waveformData.forEachIndexed { index, segment ->
        val currentX = index * segmentWidth

        val displayAmplitude = if (dynamicNormalizationEnabled) {
            val valueForDynamicNormalization = (segment.min + segment.max) / 2f
            if (amplitudeDisplayRange == 0f) {
                if (overallMinAvg == 0f) 0f else valueForDynamicNormalization // overallMaxAvg was overallMinAvg
            } else {
                (((valueForDynamicNormalization - overallMinAvg) / amplitudeDisplayRange) * 2f - 1f).coerceIn(
                    -1f,
                    1f
                )
            }
        } else {
            if (abs(segment.max) >= abs(segment.min)) segment.max else segment.min
        }

        val currentY = (drawingMiddleY - (displayAmplitude * maxAmplitudePx)).coerceIn(
            0.0f,
            canvasHeight
        )

        drawCircle(
            color = pointColor,
            radius = pointRadiusPx,
            center = Offset(currentX, currentY)
        )

        if (index > 0 && prevX != null && prevY != null) {
            drawLine(
                color = lineColor,
                start = Offset(prevX, prevY),
                end = Offset(currentX, currentY),
                strokeWidth = strokeWidthPx
            )
        }
        prevX = currentX
        prevY = currentY
    }
}

private fun DrawScope.drawProgressLine(
    canvasWidth: Float,
    canvasHeight: Float,
    progressLineColor: Color,
    currentPositionFraction: Float,
    progressLineWidthPx: Float,
) {
    val lineX = (currentPositionFraction.coerceIn(0f, 1f) * canvasWidth)
    drawLine(
        color = progressLineColor,
        start = Offset(lineX, 0.0f),
        end = Offset(lineX, canvasHeight),
        strokeWidth = progressLineWidthPx
    )
}

/**
 * Calculates the overall minimum and maximum average amplitudes for the waveform data.
 *
 */
private fun calculateAmplitudeRange(
    waveformData: List<WaveformSegment>,
    dynamicNormalizationEnabled: Boolean,
): Pair<Float, Float> {
    return if (dynamicNormalizationEnabled && waveformData.isNotEmpty()) {
        val avgAmplitudes = waveformData.map { (it.min + it.max) / 2f }
        (avgAmplitudes.minOrNull() ?: -1f) to (avgAmplitudes.maxOrNull() ?: 1f)
    } else {
        -1f to 1f
    }
}
