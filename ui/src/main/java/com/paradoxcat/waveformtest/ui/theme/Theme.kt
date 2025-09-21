package com.paradoxcat.waveformtest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.paradoxcat.waveformtest.ui.R

/**
 * Cool theme inspired by my kitchen furniture pallet (olive + light wood)
 **/
@Composable
fun ParadoxWaveViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val lightOliveWoodColorScheme = lightColorScheme(
        primary = colorResource(id = R.color.olive_dark_green),
        onPrimary = Color.White,
        secondary = colorResource(id = R.color.wood_saddle_brown),
        onSecondary = Color.White,
        tertiary = colorResource(id = R.color.olive_lighter),
        onTertiary = Color.Black,
        background = colorResource(id = R.color.wood_beige_light),
        onBackground = colorResource(id = R.color.wood_dark_brown),
        surface = colorResource(id = R.color.wood_burlywood),
        onSurface = colorResource(id = R.color.wood_dark_brown)
    )

    val darkOliveWoodColorScheme = darkColorScheme(
        primary = colorResource(id = R.color.olive_lighter),
        onPrimary = Color.Black,
        secondary = colorResource(id = R.color.wood_burlywood),
        onSecondary = Color.Black,
        tertiary = colorResource(id = R.color.olive_dark_green),
        onTertiary = colorResource(id = R.color.light_beige_text),
        background = colorResource(id = R.color.dark_olive_background),
        onBackground = colorResource(id = R.color.light_beige_text),
        surface = colorResource(id = R.color.dark_olive_surface),
        onSurface = colorResource(id = R.color.light_beige_text)
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
