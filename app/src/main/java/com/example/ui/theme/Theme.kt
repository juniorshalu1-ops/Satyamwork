package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Navy800,
    onPrimary = PureWhite,
    primaryContainer = Navy100,
    onPrimaryContainer = Navy900,
    secondary = Gold700,
    onSecondary = Navy900,
    secondaryContainer = Gold100,
    onSecondaryContainer = Gold900,
    tertiary = Teal700,
    onTertiary = PureWhite,
    tertiaryContainer = Teal100,
    onTertiaryContainer = Color(0xFF001F24),
    background = OffWhite,
    onBackground = Color(0xFF191C1E),
    surface = PureWhite,
    onSurface = Color(0xFF191C1E),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF41484D),
    outline = OutlineLight,
    error = ErrorRed,
    onError = PureWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = Navy50,
    secondary = Gold500,
    onSecondary = Color(0xFF432C00),
    secondaryContainer = Gold800,
    onSecondaryContainer = Gold100,
    tertiary = Color(0xFF4DD0E1),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Teal700,
    onTertiaryContainer = Color(0xFFB2EBF2),
    background = DarkBackground,
    onBackground = Color(0xFFE2E2E6),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC0C8D0),
    outline = DarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun ShikshakSathiTheme(
    darkTheme: Boolean = false, // Force consistent, high-contrast, beautiful light theme so dark mode on phones doesn't invert and break readability
    content: @Composable () -> Unit
) {
    // Keep consistent, crisp Bihar portal palette (Navy, Gold, Crisp White cards) on all devices
    val colorScheme = LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
