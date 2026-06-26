package org.bettamind.shared.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class BettamindThemeMode {
    Light,
    Dark,
    Calm,
    HighContrast,
}

@Composable
fun BettamindTheme(
    themeMode: BettamindThemeMode? = null,
    useAccessibleTypography: Boolean = false,
    content: @Composable () -> Unit,
) {
    val resolvedThemeMode = themeMode ?: if (isSystemInDarkTheme()) {
        BettamindThemeMode.Dark
    } else {
        BettamindThemeMode.Light
    }
    MaterialTheme(
        colorScheme = resolvedThemeMode.colorScheme(),
        typography = bettamindTypography(useAccessibleTypography),
        content = content,
    )
}

private fun BettamindThemeMode.colorScheme() = when (this) {
    BettamindThemeMode.Light -> lightColorScheme(
        primary = colorFromRgb(BettamindColorTokens.Primary),
        onPrimary = colorFromRgb(BettamindColorTokens.OnPrimary),
        secondary = colorFromRgb(BettamindColorTokens.Secondary),
        onSecondary = colorFromRgb(BettamindColorTokens.OnSecondary),
        tertiary = colorFromRgb(BettamindColorTokens.Accent),
        background = colorFromRgb(BettamindColorTokens.Background),
        onBackground = colorFromRgb(BettamindColorTokens.OnBackground),
        surface = colorFromRgb(BettamindColorTokens.Surface),
        onSurface = colorFromRgb(BettamindColorTokens.OnSurface),
        surfaceVariant = colorFromRgb(BettamindColorTokens.SurfaceVariant),
        onSurfaceVariant = colorFromRgb(BettamindColorTokens.OnSurfaceVariant),
        outline = colorFromRgb(BettamindColorTokens.Outline),
        error = colorFromRgb(BettamindColorTokens.Danger),
        onError = colorFromRgb(BettamindColorTokens.OnDanger),
    )

    BettamindThemeMode.Calm -> lightColorScheme(
        primary = colorFromRgb(BettamindColorTokens.Secondary),
        onPrimary = colorFromRgb(BettamindColorTokens.OnSecondary),
        secondary = colorFromRgb(BettamindColorTokens.Primary),
        onSecondary = colorFromRgb(BettamindColorTokens.OnPrimary),
        tertiary = colorFromRgb(BettamindColorTokens.Accent),
        background = colorFromRgb(BettamindColorTokens.CalmBackground),
        onBackground = colorFromRgb(BettamindColorTokens.OnBackground),
        surface = colorFromRgb(BettamindColorTokens.CalmSurface),
        onSurface = colorFromRgb(BettamindColorTokens.OnSurface),
        surfaceVariant = colorFromRgb(BettamindColorTokens.SurfaceVariant),
        onSurfaceVariant = colorFromRgb(BettamindColorTokens.OnSurfaceVariant),
        outline = colorFromRgb(BettamindColorTokens.Outline),
        error = colorFromRgb(BettamindColorTokens.Danger),
        onError = colorFromRgb(BettamindColorTokens.OnDanger),
    )

    BettamindThemeMode.Dark -> darkColorScheme(
        primary = colorFromRgb(BettamindColorTokens.DarkPrimary),
        onPrimary = colorFromRgb(BettamindColorTokens.DarkOnPrimary),
        secondary = colorFromRgb(BettamindColorTokens.DarkSecondary),
        onSecondary = colorFromRgb(BettamindColorTokens.DarkOnSecondary),
        tertiary = colorFromRgb(BettamindColorTokens.Accent),
        background = colorFromRgb(BettamindColorTokens.DarkBackground),
        onBackground = colorFromRgb(BettamindColorTokens.DarkOnSurface),
        surface = colorFromRgb(BettamindColorTokens.DarkSurface),
        onSurface = colorFromRgb(BettamindColorTokens.DarkOnSurface),
        surfaceVariant = colorFromRgb(BettamindColorTokens.DarkSurfaceVariant),
        onSurfaceVariant = colorFromRgb(BettamindColorTokens.DarkOnSurfaceVariant),
        outline = colorFromRgb(BettamindColorTokens.DarkOutline),
        error = colorFromRgb(BettamindColorTokens.DarkDanger),
        onError = colorFromRgb(BettamindColorTokens.DarkOnDanger),
    )

    BettamindThemeMode.HighContrast -> lightColorScheme(
        primary = colorFromRgb(BettamindColorTokens.HighContrastPrimary),
        onPrimary = colorFromRgb(BettamindColorTokens.HighContrastOnPrimary),
        secondary = colorFromRgb(BettamindColorTokens.HighContrastSecondary),
        onSecondary = colorFromRgb(BettamindColorTokens.HighContrastOnSecondary),
        tertiary = colorFromRgb(BettamindColorTokens.HighContrastPrimary),
        background = colorFromRgb(BettamindColorTokens.HighContrastBackground),
        onBackground = colorFromRgb(BettamindColorTokens.HighContrastOnBackground),
        surface = colorFromRgb(BettamindColorTokens.HighContrastBackground),
        onSurface = colorFromRgb(BettamindColorTokens.HighContrastOnBackground),
        surfaceVariant = colorFromRgb(BettamindColorTokens.HighContrastSurfaceVariant),
        onSurfaceVariant = colorFromRgb(BettamindColorTokens.HighContrastOnBackground),
        outline = colorFromRgb(BettamindColorTokens.HighContrastOutline),
        error = colorFromRgb(BettamindColorTokens.HighContrastDanger),
        onError = colorFromRgb(BettamindColorTokens.HighContrastOnPrimary),
    )
}

// Keep launch typography resource-free while the iOS TestFlight abort is isolated.
@Suppress("UNUSED_PARAMETER")
@Composable
private fun bettamindTypography(useAccessibleTypography: Boolean): Typography = Typography()
