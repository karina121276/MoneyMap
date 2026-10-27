package com.thecorporatedeveloper.moneymap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MoneyMapBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = MoneyMapGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    error = MoneyMapRed,
    background = SurfaceDark,
    surface = SurfaceCardDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = MoneyMapBlue,
    onPrimary = Color.White,
    primaryContainer = MoneyMapBlueLight,
    onPrimaryContainer = MoneyMapNavy,
    secondary = MoneyMapGreen,
    onSecondary = Color.White,
    secondaryContainer = MoneyMapGreenLight,
    onSecondaryContainer = Color(0xFF065F46),
    error = MoneyMapRed,
    background = SurfaceLight,
    surface = SurfaceCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary
)

@Composable
fun MoneyMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (accentColor != null) {
        baseScheme.copy(primary = accentColor)
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoneyMapTypography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MoneyMapTheme(darkTheme = darkTheme, content = content)
}
