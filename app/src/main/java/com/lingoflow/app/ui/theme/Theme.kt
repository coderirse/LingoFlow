package com.lingoflow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.lingoflow.app.domain.model.settings.InterfaceStyle
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

private val EditorialDarkColorScheme = darkColorScheme(
    primary = EditorialDarkPrimary,
    secondary = EditorialDarkSecondary,
    background = EditorialDarkBackground,
    surface = EditorialDarkSurface,
    surfaceVariant = EditorialDarkElevated,
    surfaceContainerLow = EditorialDarkSurface,
    surfaceContainerHigh = EditorialDarkElevated,
    outline = EditorialDarkOutline,
    onPrimary = EditorialDarkBackground,
    onSecondary = EditorialDarkBackground,
    onBackground = EditorialDarkOnSurface,
    onSurface = EditorialDarkOnSurface,
    onSurfaceVariant = EditorialDarkOnSurfaceVariant
)

private val EditorialLightColorScheme = lightColorScheme(
    primary = EditorialLightPrimary,
    secondary = EditorialLightSecondary,
    background = EditorialLightBackground,
    surface = EditorialLightSurface,
    surfaceVariant = EditorialLightElevated,
    surfaceContainerLow = EditorialLightSurface,
    surfaceContainerHigh = EditorialLightElevated,
    outline = EditorialLightOutline,
    onPrimary = EditorialLightSurface,
    onSecondary = EditorialLightSurface,
    onBackground = EditorialLightOnSurface,
    onSurface = EditorialLightOnSurface,
    onSurfaceVariant = EditorialLightOnSurfaceVariant
)

@Composable
fun LingoFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    interfaceStyle: InterfaceStyle = InterfaceStyle.MODERN,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when (interfaceStyle) {
        InterfaceStyle.MODERN -> if (darkTheme) DarkColorScheme else LightColorScheme
        InterfaceStyle.EDITORIAL ->
            if (darkTheme) EditorialDarkColorScheme else EditorialLightColorScheme
    }
    val typography = when (interfaceStyle) {
        InterfaceStyle.MODERN -> Typography
        InterfaceStyle.EDITORIAL -> EditorialTypography
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
