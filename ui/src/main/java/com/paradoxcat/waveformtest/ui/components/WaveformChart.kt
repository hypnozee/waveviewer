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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paradoxcat.waveformtest.domain.model.WaveformSegment
import kotlin.collections.forEachIndexed
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.maxOrNull
import kotlin.collections.minOrNull
import kotlin.math.abs
import kotlin.ranges.coerceIn
import kotlin.to

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
    pointRadiusDp: Dp = 2.dp,
    strokeWidthDp: Dp = 1.5.dp,
    progressLineWidthDp: Dp = 1.5.dp,
    horizontalPaddin: Dp = 8.dp,
    verticalPadding: Dp = 8.dp,
) {
    // Convert Dp values to Px for accuracy.
    val leftRightPaddingPx = with(LocalDensity.current) { horizontalPaddin.toPx() }
    val topBottomPaddingPx = with(LocalDensity.current) { verticalPadding.toPx() }
    val pointRadiusPx = with(LocalDensity.current) { pointRadiusDp.toPx() }
    val strokeWidthPx = with(LocalDensity.current) { strokeWidthDp.toPx() }
    val progressLineWidthPx = with(LocalDensity.current) { progressLineWidthDp.toPx() }

    val (overallMinAvg, overallMaxAvg) = remember(waveformData, dynamicNormalizationEnabled) {
        if (dynamicNormalizationEnabled && waveformData.isNotEmpty()) {
            val avgAmplitudes = waveformData.map { (it.min + it.max) / 2f }
            (avgAmplitudes.minOrNull() ?: -1f) to (avgAmplitudes.maxOrNull() ?: 1f)
        } else {
            -1f to 1f
        }
    }

    var valueRange = overallMaxAvg - overallMinAvg
    if (valueRange < 0.00001f) { // Avoid division by zero
        valueRange =
            0f
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val totalCanvasWidth = size.width.toFloat()
                        val drawingWidth = totalCanvasWidth - 2 * leftRightPaddingPx
                        if (drawingWidth > 0) {
                            val pointX = offset.x
                            val positionFraction =
                                ((pointX - leftRightPaddingPx) / drawingWidth).coerceIn(0f, 1f)
                            onSeekIntent(positionFraction)
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        val totalCanvasWidth = size.width.toFloat()
                        val drawingWidth = totalCanvasWidth - 2 * leftRightPaddingPx
                        if (drawingWidth > 0) {
                            val pointX = change.position.x
                            val positionFraction =
                                ((pointX - leftRightPaddingPx) / drawingWidth).coerceIn(0f, 1f)
                            onSeekIntent(positionFraction)
                            change.consume()
                        }
                    }
                )
            }
    ) {
        val totalCanvasWidth = size.width
        val totalCanvasHeight = size.height
        val drawingWidth = totalCanvasWidth - 2 * leftRightPaddingPx
        val drawingHeight = totalCanvasHeight - 2 * topBottomPaddingPx

        if (drawingWidth <= 0f || drawingHeight <= 0f || waveformData.isEmpty()) {
            val middleY = totalCanvasHeight / 2f
            // Draw a horizontal line in the middle as a placeholder.
            drawLine(
                color = Color.Gray,
                start = Offset(leftRightPaddingPx, middleY),
                end = Offset(totalCanvasWidth - leftRightPaddingPx, middleY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            if (drawingWidth > 0f && drawingHeight > 0f) {
                val lineX =
                    leftRightPaddingPx + (currentPositionFraction.coerceIn(0f, 1f) * drawingWidth)
                drawLine(
                    color = progressLineColor,
                    start = Offset(lineX, topBottomPaddingPx),
                    end = Offset(lineX, totalCanvasHeight - topBottomPaddingPx),
                    strokeWidth = progressLineWidthPx
                )
            }
            return@Canvas
        }

        val numSegments = waveformData.size
        // Calculate the width for each segment on the canvas.
        val segmentWidth = if (numSegments > 1) drawingWidth / (numSegments - 1) else drawingWidth
        val drawingMiddleY =
            topBottomPaddingPx + drawingHeight / 2f
        val maxAmplitudePx = drawingHeight / 2f

        var prevX: Float? = null
        var prevY: Float? = null

        waveformData.forEachIndexed { index, segment ->
            val currentX =
                leftRightPaddingPx + (index * segmentWidth)

            val displayAmplitude = if (dynamicNormalizationEnabled) {
                val valueForDynamicNormalization = (segment.min + segment.max) / 2f
                if (valueRange == 0f) {
                    if (overallMinAvg == 0f && overallMaxAvg == 0f) 0f else valueForDynamicNormalization
                } else {
                    // Normalize the current segment's average amplitude to the -1f to 1f range.
                    (((valueForDynamicNormalization - overallMinAvg) / valueRange) * 2f - 1f).coerceIn(
                        -1f,
                        1f
                    )
                }
            } else {
                if (abs(segment.max) >= abs(segment.min)) segment.max else segment.min
            }

            val currentY = (drawingMiddleY - (displayAmplitude * maxAmplitudePx)).coerceIn(
                topBottomPaddingPx,
                totalCanvasHeight - topBottomPaddingPx
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

        val lineX = leftRightPaddingPx + (currentPositionFraction.coerceIn(0f, 1f) * drawingWidth)
        drawLine(
            color = progressLineColor,
            start = Offset(lineX, topBottomPaddingPx),
            end = Offset(lineX, totalCanvasHeight - topBottomPaddingPx),
            strokeWidth = progressLineWidthPx
        )
    }
}
