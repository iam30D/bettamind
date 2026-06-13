package org.bettamind.shared.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.bettamind.shared.generated.resources.Res
import org.bettamind.shared.generated.resources.*
import org.jetbrains.compose.resources.Font

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

@Composable
private fun bettamindTypography(useAccessibleTypography: Boolean): Typography {
    val family = if (useAccessibleTypography) {
        FontFamily(
            Font(Res.font.atkinson_hyperlegible_regular, weight = FontWeight.Normal),
            Font(Res.font.atkinson_hyperlegible_bold, weight = FontWeight.Bold),
            Font(Res.font.noto_sans_variable, weight = FontWeight.Normal),
            Font(Res.font.noto_sans_arabic_variable, weight = FontWeight.Normal),
            Font(Res.font.noto_sans_devanagari_variable, weight = FontWeight.Normal),
            Font(Res.font.noto_sans_sc_variable, weight = FontWeight.Normal),
        )
    } else {
        FontFamily(
            Font(Res.font.noto_sans_variable, weight = FontWeight.Normal),
            Font(Res.font.noto_sans_variable, weight = FontWeight.Medium),
            Font(Res.font.noto_sans_variable, weight = FontWeight.SemiBold),
            Font(Res.font.noto_sans_variable, weight = FontWeight.Bold),
            Font(Res.font.noto_sans_arabic_variable, weight = FontWeight.Normal),
            Font(Res.font.noto_sans_devanagari_variable, weight = FontWeight.Normal),
            Font(Res.font.noto_sans_sc_variable, weight = FontWeight.Normal),
        )
    }

    return Typography().withFontFamily(family)
}

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)
