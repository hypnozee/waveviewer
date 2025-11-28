package com.waveform.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.waveform.ui.R

/**
 * Cool theme inspired by my kitchen furniture pallet (olive + light wood)
 **/
@Composable
fun WaveViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val lightOliveWoodColorScheme = lightColorScheme(
        primary = colorResource(id = R.color.olive_dark_green),
        onPrimary = colorResource(id = R.color.wood_beige_light),
        secondary = colorResource(id = R.color.wood_saddle_brown),
        onSecondary = colorResource(id = R.color.wood_beige_light),
        tertiary = colorResource(id = R.color.olive_lighter),
        onTertiary = colorResource(id = R.color.dark_olive_background),
        background = colorResource(id = R.color.wood_beige_light),
        onBackground = colorResource(id = R.color.wood_dark_brown),
        surface = colorResource(id = R.color.wood_beige_dim),
        onSurface = colorResource(id = R.color.wood_dark_brown),
        surfaceVariant = colorResource(id = R.color.wood_beige_dimmer),
        onSurfaceVariant = colorResource(id = R.color.wood_dark_brown),
        surfaceContainer = colorResource(id = R.color.light_container_color)
    )

    val darkOliveWoodColorScheme = darkColorScheme(
        primary = colorResource(id = R.color.olive_lighter),
        onPrimary = colorResource(id = R.color.light_beige_text),
        secondary = colorResource(id = R.color.wood_burlywood),
        onSecondary = colorResource(id = R.color.light_beige_text),
        tertiary = colorResource(id = R.color.olive_dark_green),
        onTertiary = colorResource(id = R.color.light_beige_text),
        background = colorResource(id = R.color.dark_olive_background),
        onBackground = colorResource(id = R.color.light_beige_text),
        surface = colorResource(id = R.color.dark_olive_surface),
        onSurface = colorResource(id = R.color.light_beige_text),
        surfaceVariant = colorResource(id = R.color.dark_surface_variant_color),
        onSurfaceVariant = colorResource(id = R.color.light_beige_text),
        surfaceContainer = colorResource(id = R.color.dark_container_color)
    )

    val colorScheme = if (darkTheme) {
        darkOliveWoodColorScheme
    } else {
        lightOliveWoodColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}