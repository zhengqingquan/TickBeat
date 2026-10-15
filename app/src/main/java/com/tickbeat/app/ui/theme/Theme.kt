package com.tickbeat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF0F2740)
private val Ink = Color(0xFF1B3A57)
private val Sand = Color(0xFFF4F0E6)
private val Accent = Color(0xFFF2C14E)
private val AccentDeep = Color(0xFFE09F1F)
private val BeatIdle = Color(0xFF8FA3B5)
private val BeatActive = Color(0xFFF2C14E)

val TickBeatBeatIdle = BeatIdle
val TickBeatBeatActive = BeatActive

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Navy,
    tertiary = AccentDeep,
    background = Sand,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFE8E2D6),
    onSurfaceVariant = Ink
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Navy,
    secondary = AccentDeep,
    onSecondary = Color.White,
    background = Color(0xFF0B1A2A),
    onBackground = Sand,
    surface = Color(0xFF13283D),
    onSurface = Sand,
    surfaceVariant = Ink,
    onSurfaceVariant = Color(0xFFC9D4DF)
)

@Composable
fun TickBeatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
