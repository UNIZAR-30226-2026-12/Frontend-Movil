package com.example.random_reversi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = BgColor,
    surface = SurfaceColor,
    onPrimary = BgColor,
    onSecondary = BgColor,
    onBackground = TextColor,
    onSurface = TextColor,
    surfaceVariant = SurfaceLightColor,
    outline = BorderColor
)

@Composable
fun ReversiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
