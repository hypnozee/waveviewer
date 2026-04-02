package com.waveform.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.waveform.domain.model.WaveformSegment
import com.waveform.ui.theme.WaveViewerTheme
import kotlin.math.abs

private const val MIN_AMPLITUDE_RANGE = 0.00001f

/**
 * Chart to see a series of connected points.
 * Has a vertical line to indicate current play position.
 * You can drag the line to change the play position.
 * Has two view modes:
 * 1. True Amplitude: displays segments based on their value inside [-1.0, +1.0]
 * 2. Dynamic Normalization: scale the waveform to fill the height based on its actual peaks.
 */
@SuppressLint("DefaultLocale")
@Composable
fun WaveformChart(
    waveformData: List<WaveformSegment>,
    playProgress: Float,
    dynamicNormalizationEnabled: Boolean,
    yMin: Float,
    yMax: Float,
    modifier: Modifier = Modifier,
    onDragStarted: () -> Unit = {},
    onSeekIntent: (Float) -> Unit,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    pointColor: Color = MaterialTheme.colorScheme.secondary,
    progressLineColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    // Local position shown while dragging; null when not dragging
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val displayProgress = dragProgress ?: playProgress

    val lineWidth = with(LocalDensity.current) { 1.dp.toPx() }
    val pointSize = with(LocalDensity.current) { 2.dp.toPx() }
    val progressLineWidth = with(LocalDensity.current) { 3.dp.toPx() }

    var amplitudeDisplayRange = yMax - yMin
    if (amplitudeDisplayRange < MIN_AMPLITUDE_RANGE) { // Avoid division by zero
        amplitudeDisplayRange = 0f
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onDragStarted()
                        if (size.width > 0) {
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        if (size.width > 0) {
                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        dragProgress?.let { onSeekIntent(it) }
                        dragProgress = null
                    },
                    onDragCancel = {
                        dragProgress = null
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        drawWaveform(
            waveformData = waveformData,
            dynamicNormalizationEnabled = dynamicNormalizationEnabled,
            overallMin = yMin,
            amplitudeDisplayRange = amplitudeDisplayRange,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            pointColor = pointColor,
            pointRadiusPx = pointSize,
            lineColor = lineColor,
            strokeWidthPx = lineWidth
        )

        drawProgressLine(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            progressLineColor = progressLineColor,
            currentPositionFraction = displayProgress,
            progressLineWidthPx = progressLineWidth
        )
    }
}

private fun DrawScope.drawWaveform(
    waveformData: List<WaveformSegment>,
    dynamicNormalizationEnabled: Boolean,
    overallMin: Float,
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
            val valueForNormalization =
                if (abs(segment.max) >= abs(segment.min)) segment.max else segment.min
            if (amplitudeDisplayRange == 0f) {
                valueForNormalization
            } else {
                (((valueForNormalization - overallMin) / amplitudeDisplayRange) * 2f - 1f).coerceIn(
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


@PreviewLightDark
@Composable
fun WaveformChartPreview() {
    WaveViewerTheme {
        val waveformData = listOf(
            WaveformSegment(0.0f, 0.5f),
            WaveformSegment(-0.2f, 0.3f),
            WaveformSegment(0.1f, 0.8f),
            WaveformSegment(-0.5f, 0.0f),
            WaveformSegment(0.3f, 0.6f)
        )
        WaveformChart(
            waveformData = waveformData,
            playProgress = 0.5f,
            dynamicNormalizationEnabled = true,
            yMin = -0.5f,
            yMax = 0.8f,
            modifier = Modifier.fillMaxSize(),
            onSeekIntent = {}
        )
    }
}
