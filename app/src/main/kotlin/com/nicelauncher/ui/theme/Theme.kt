package com.nicelauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    background = Color(0xFF141416),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    primary = Color.White,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color.White.copy(alpha = 0.15f),
)

private val LightColors = lightColorScheme(
    background = Color(0xFFF5F5F3),
    surface = Color(0xFFEAEAEA),
    surfaceVariant = Color(0xFFE5E5EA),
    primary = Color.Black,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    outline = Color.Black.copy(alpha = 0.12f),
)

@Composable
fun NiceLauncherTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
