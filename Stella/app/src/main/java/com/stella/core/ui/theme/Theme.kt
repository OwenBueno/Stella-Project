package com.stella.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StellaDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Warning,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextMuted,
    error = Primary,
    onError = Color.White,
)

@Composable
fun StellaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StellaDarkColorScheme,
        typography = StellaTypography,
        content = content,
    )
}
