package com.example.random_reversi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Usamos lightColorScheme porque ahora las superficies son claras (beige/crema)
private val CartoonColorScheme = lightColorScheme(
    primary          = PrimaryColor,
    secondary        = SecondaryColor,
    background       = BgColor,
    surface          = SurfaceColor,
    onPrimary        = TextOnDark,
    onSecondary      = TextColor,
    onBackground     = TextOnDark,
    onSurface        = TextColor,
    surfaceVariant   = SurfaceLightColor,
    outline          = BorderColor
)

@Composable
fun ReversiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CartoonColorScheme,
        content = content
    )
}
