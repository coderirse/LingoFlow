package com.lingoflow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LingoFlowPrimary,
    secondary = LingoFlowSecondary,
    background = LingoFlowBackground,
    surface = LingoFlowSurface,
    surfaceVariant = LingoFlowSurfaceElevated,
    surfaceContainerLow = LingoFlowSurface,
    surfaceContainerHigh = LingoFlowSurfaceElevated,
    onPrimary = LingoFlowOnSurface,
    onSecondary = LingoFlowOnSurface,
    onBackground = LingoFlowOnSurface,
    onSurface = LingoFlowOnSurface,
    onSurfaceVariant = LingoFlowOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LingoFlowPrimary,
    secondary = LingoFlowSecondary
)

@Composable
fun LingoFlowTheme(
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
