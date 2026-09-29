package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrickPrimary,
    onPrimary = BrickOnPrimary,
    primaryContainer = BrickContainer,
    onPrimaryContainer = BrickOnContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    background = PaperBackground,
    onBackground = InkDark,
    surface = PaperSurface,
    onSurface = InkDark,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkMedium,
    outline = OutlineColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force bright light theme as explicitly requested by user
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
