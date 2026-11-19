package com.devicexpert.app.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalUseFahrenheit = staticCompositionLocalOf { false }

private val OledDarkColorScheme = darkColorScheme(
    primary = CyanPulse,
    secondary = NeonViolet,
    tertiary = EmeraldVolt,
    background = OledBlack,
    surface = CharcoalCard,
    surfaceVariant = CharcoalElevated,
    onPrimary = OledBlack,
    onSecondary = TextPrimary,
    onTertiary = OledBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    error = StatusCriticalCrimson
)

private val CleanLightColorScheme = lightColorScheme(
    primary = CyanPulse,
    secondary = NeonViolet,
    tertiary = EmeraldVolt,
    background = LightCanvas,
    surface = LightCard,
    surfaceVariant = LightElevated,
    onPrimary = LightCard,
    onSecondary = LightCard,
    onTertiary = LightCard,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = StatusCriticalCrimson
)

@Composable
fun DeviceXpertTheme(
    darkTheme: Boolean = true,
    useFahrenheit: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OledDarkColorScheme else CleanLightColorScheme

    CompositionLocalProvider(LocalUseFahrenheit provides useFahrenheit) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
