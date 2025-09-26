package com.paradoxcat.waveformtest.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.paradoxcat.waveformtest.ui.theme.ParadoxWaveViewerTheme

@Composable
fun NormalizationToggle(
    dynamicNormalizationEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = !dynamicNormalizationEnabled,
                        onClick = { if (dynamicNormalizationEnabled) onToggle() },
                    )
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                RadioButtonOption(
                    text = "True Amplitude",
                    isSelected = !dynamicNormalizationEnabled
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = dynamicNormalizationEnabled,
                        onClick = { if (!dynamicNormalizationEnabled) onToggle() },
                    )
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                RadioButtonOption(
                    text = "Dynamic Height",
                    isSelected = dynamicNormalizationEnabled
                )
            }
        }
    }
}

@Composable
private fun RadioButtonOption(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@PreviewLightDark
@Composable
fun NormalizationTogglePreviewTrueAmplitude() {
    ParadoxWaveViewerTheme {
        NormalizationToggle(dynamicNormalizationEnabled = false, onToggle = {})
    }
}

@PreviewLightDark
@Composable
fun NormalizationTogglePreviewFillHeight() {
    ParadoxWaveViewerTheme {
        NormalizationToggle(dynamicNormalizationEnabled = true, onToggle = {})
    }
}

