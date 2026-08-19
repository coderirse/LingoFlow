package com.lingoflow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.lingoflow.app.domain.model.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = LingoFlowPrimary,
    secondary = LingoFlowSecondary,
    background = LingoFlowBackground,
    surface = LingoFlowSurface,
    surfaceVariant = LingoFlowSurfaceElevated,
    surfaceContainerLow = LingoFlowSurface,
    surfaceContainerHigh = LingoFlowSurfaceElevated,
    outline = LingoFlowOutlineDark,
    onPrimary = LingoFlowOnSurface,
    onSecondary = LingoFlowOnSurface,
    onBackground = LingoFlowOnSurface,
    onSurface = LingoFlowOnSurface,
    onSurfaceVariant = LingoFlowOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LingoFlowPrimaryLight,
    secondary = LingoFlowSecondaryLight,
    background = LingoFlowLightBackground,
    surface = LingoFlowLightSurface,
    surfaceVariant = LingoFlowLightSurfaceElevated,
    surfaceContainerLow = LingoFlowLightSurface,
    surfaceContainerHigh = LingoFlowLightSurfaceElevated,
    outline = LingoFlowOutlineLight,
    onPrimary = LingoFlowLightSurface,
    onSecondary = LingoFlowLightSurface,
    onBackground = LingoFlowLightOnSurface,
    onSurface = LingoFlowLightOnSurface,
    onSurfaceVariant = LingoFlowLightOnSurfaceVariant
)

@Composable
fun LingoFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
