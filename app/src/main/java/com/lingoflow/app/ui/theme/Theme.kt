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
    onPrimary = LingoFlowOnPrimary,
    primaryContainer = LingoFlowPrimaryContainer,
    onPrimaryContainer = LingoFlowOnPrimaryContainer,
    secondary = LingoFlowSecondary,
    onSecondary = LingoFlowOnSecondary,
    secondaryContainer = LingoFlowSecondaryContainer,
    onSecondaryContainer = LingoFlowOnSecondaryContainer,
    tertiary = LingoFlowTertiary,
    onTertiary = LingoFlowOnTertiary,
    tertiaryContainer = LingoFlowTertiaryContainer,
    onTertiaryContainer = LingoFlowOnTertiaryContainer,
    error = LingoFlowErrorDark,
    onError = LingoFlowOnErrorDark,
    errorContainer = LingoFlowErrorContainerDark,
    onErrorContainer = LingoFlowOnErrorContainerDark,
    background = LingoFlowBackground,
    onBackground = LingoFlowOnSurface,
    surface = LingoFlowSurface,
    onSurface = LingoFlowOnSurface,
    surfaceVariant = LingoFlowSurfaceElevated,
    onSurfaceVariant = LingoFlowOnSurfaceVariant,
    surfaceContainerLowest = LingoFlowSurfaceLowest,
    surfaceContainerLow = LingoFlowSurfaceLow,
    surfaceContainer = LingoFlowSurface,
    surfaceContainerHigh = LingoFlowSurfaceHigh,
    surfaceContainerHighest = LingoFlowSurfaceHighest,
    outline = LingoFlowOutlineDark,
    outlineVariant = LingoFlowOutlineVariantDark,
    inverseSurface = LingoFlowInverseSurfaceDark,
    inverseOnSurface = LingoFlowInverseOnSurfaceDark,
    inversePrimary = LingoFlowInversePrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = LingoFlowPrimaryLight,
    onPrimary = LingoFlowOnPrimaryLight,
    primaryContainer = LingoFlowPrimaryContainerLight,
    onPrimaryContainer = LingoFlowOnPrimaryContainerLight,
    secondary = LingoFlowSecondaryLight,
    onSecondary = LingoFlowOnSecondaryLight,
    secondaryContainer = LingoFlowSecondaryContainerLight,
    onSecondaryContainer = LingoFlowOnSecondaryContainerLight,
    tertiary = LingoFlowTertiaryLight,
    onTertiary = LingoFlowOnTertiaryLight,
    tertiaryContainer = LingoFlowTertiaryContainerLight,
    onTertiaryContainer = LingoFlowOnTertiaryContainerLight,
    error = LingoFlowErrorLight,
    onError = LingoFlowOnErrorLight,
    errorContainer = LingoFlowErrorContainerLight,
    onErrorContainer = LingoFlowOnErrorContainerLight,
    background = LingoFlowLightBackground,
    onBackground = LingoFlowLightOnSurface,
    surface = LingoFlowLightSurface,
    onSurface = LingoFlowLightOnSurface,
    surfaceVariant = LingoFlowLightSurfaceElevated,
    onSurfaceVariant = LingoFlowLightOnSurfaceVariant,
    surfaceContainerLowest = LingoFlowLightSurfaceLowest,
    surfaceContainerLow = LingoFlowLightSurface,
    surfaceContainer = LingoFlowLightSurface,
    surfaceContainerHigh = LingoFlowLightSurfaceHigh,
    surfaceContainerHighest = LingoFlowLightSurfaceHighest,
    outline = LingoFlowOutlineLight,
    outlineVariant = LingoFlowOutlineVariantLight,
    inverseSurface = LingoFlowInverseSurfaceLight,
    inverseOnSurface = LingoFlowInverseOnSurfaceLight,
    inversePrimary = LingoFlowInversePrimaryLight
)

private val EditorialDarkColorScheme = darkColorScheme(
    primary = EditorialDarkPrimary,
    onPrimary = EditorialDarkOnPrimary,
    primaryContainer = EditorialDarkPrimaryContainer,
    onPrimaryContainer = EditorialDarkOnPrimaryContainer,
    secondary = EditorialDarkSecondary,
    onSecondary = EditorialDarkOnSecondary,
    secondaryContainer = EditorialDarkSecondaryContainer,
    onSecondaryContainer = EditorialDarkOnSecondaryContainer,
    tertiary = EditorialDarkTertiary,
    onTertiary = EditorialDarkOnTertiary,
    tertiaryContainer = EditorialDarkTertiaryContainer,
    onTertiaryContainer = EditorialDarkOnTertiaryContainer,
    error = EditorialDarkError,
    onError = EditorialDarkOnError,
    errorContainer = EditorialDarkErrorContainer,
    onErrorContainer = EditorialDarkOnErrorContainer,
    background = EditorialDarkBackground,
    onBackground = EditorialDarkOnSurface,
    surface = EditorialDarkSurface,
    onSurface = EditorialDarkOnSurface,
    surfaceVariant = EditorialDarkElevated,
    onSurfaceVariant = EditorialDarkOnSurfaceVariant,
    surfaceContainerLowest = EditorialDarkSurfaceLowest,
    surfaceContainerLow = EditorialDarkSurfaceLow,
    surfaceContainer = EditorialDarkSurface,
    surfaceContainerHigh = EditorialDarkSurfaceHigh,
    surfaceContainerHighest = EditorialDarkSurfaceHighest,
    outline = EditorialDarkOutline,
    outlineVariant = EditorialDarkOutlineVariant,
    inverseSurface = EditorialDarkInverseSurface,
    inverseOnSurface = EditorialDarkInverseOnSurface,
    inversePrimary = EditorialDarkInversePrimary
)

private val EditorialLightColorScheme = lightColorScheme(
    primary = EditorialLightPrimary,
    onPrimary = EditorialLightOnPrimary,
    primaryContainer = EditorialLightPrimaryContainer,
    onPrimaryContainer = EditorialLightOnPrimaryContainer,
    secondary = EditorialLightSecondary,
    onSecondary = EditorialLightOnSecondary,
    secondaryContainer = EditorialLightSecondaryContainer,
    onSecondaryContainer = EditorialLightOnSecondaryContainer,
    tertiary = EditorialLightTertiary,
    onTertiary = EditorialLightOnTertiary,
    tertiaryContainer = EditorialLightTertiaryContainer,
    onTertiaryContainer = EditorialLightOnTertiaryContainer,
    error = EditorialLightError,
    onError = EditorialLightOnError,
    errorContainer = EditorialLightErrorContainer,
    onErrorContainer = EditorialLightOnErrorContainer,
    background = EditorialLightBackground,
    onBackground = EditorialLightOnSurface,
    surface = EditorialLightSurface,
    onSurface = EditorialLightOnSurface,
    surfaceVariant = EditorialLightElevated,
    onSurfaceVariant = EditorialLightOnSurfaceVariant,
    surfaceContainerLowest = EditorialLightSurfaceLowest,
    surfaceContainerLow = EditorialLightSurface,
    surfaceContainer = EditorialLightSurface,
    surfaceContainerHigh = EditorialLightSurfaceHigh,
    surfaceContainerHighest = EditorialLightSurfaceHighest,
    outline = EditorialLightOutline,
    outlineVariant = EditorialLightOutlineVariant,
    inverseSurface = EditorialLightInverseSurface,
    inverseOnSurface = EditorialLightInverseOnSurface,
    inversePrimary = EditorialLightInversePrimary
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
