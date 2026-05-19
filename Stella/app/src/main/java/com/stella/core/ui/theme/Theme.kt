package com.stella.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color

private val StellaDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Primary,
    onSecondary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextMuted,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = SurfaceCard,
    outline = Divider,
    outlineVariant = Border,
    error = Error,
    onError = Color.White,
)

@Composable
fun StellaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StellaDarkColorScheme,
        typography = StellaTypography,
    ) {
        CompositionLocalProvider(LocalContentColor provides TextPrimary) {
            content()
        }
    }
}
