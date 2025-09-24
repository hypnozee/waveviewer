package com.paradoxcat.waveformtest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { if (dynamicNormalizationEnabled) onToggle() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                RadioButton(
                    selected = !dynamicNormalizationEnabled,
                    onClick = { if (dynamicNormalizationEnabled) onToggle() }
                )
                Text("True Amplitude", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { if (!dynamicNormalizationEnabled) onToggle() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                RadioButton(
                    selected = dynamicNormalizationEnabled,
                    onClick = { if (!dynamicNormalizationEnabled) onToggle() }
                )
                Text("Fill Height", style = MaterialTheme.typography.bodyMedium)
            }
        }
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
