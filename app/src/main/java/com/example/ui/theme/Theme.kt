package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = FarmGreenDark,
    onPrimary = Color.White,
    primaryContainer = FarmGreenContainer,
    onPrimaryContainer = FarmGreenDark,
    secondary = FarmOrangeCategory,
    onSecondary = Color.White,
    tertiary = FarmBlueCategory,
    background = FarmBgCream,
    onBackground = FarmTextPrimary,
    surface = FarmSurface,
    onSurface = FarmTextPrimary,
    surfaceVariant = Color(0xFFF0EFE9),
    onSurfaceVariant = FarmTextSecondary,
    outline = FarmCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = FarmGreenMedium,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A28),
    onPrimaryContainer = FarmGreenLight,
    secondary = FarmOrangeCategory,
    tertiary = FarmBlueCategory,
    background = Color(0xFF151816),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF1E2320),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF282E2B),
    onSurfaceVariant = Color(0xFFB0B6B2),
    outline = Color(0xFF454E48)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
