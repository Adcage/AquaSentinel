package com.vision.swimsafe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = SafetyBlue,
    onPrimary = CardBackground,
    primaryContainer = SafetyBlueLight,
    onPrimaryContainer = SafetyBlueDark,
    secondary = SuccessGreen,
    onSecondary = CardBackground,
    tertiary = WarningOrange,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    error = AlarmRed,
    onError = CardBackground,
    outline = DividerColor,
)

@Composable
fun AndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content,
    )
}
