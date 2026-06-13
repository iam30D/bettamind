package org.bettamind.shared.design

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

object BettamindColorTokens {
    const val Primary = 0x0E5A7A
    const val OnPrimary = 0xFFFFFF
    const val Secondary = 0x2F7D57
    const val OnSecondary = 0xFFFFFF
    const val Accent = 0xA6C83A
    const val Background = 0xF7FAF8
    const val OnBackground = 0x12201B
    const val Surface = 0xFFFFFF
    const val OnSurface = 0x12201B
    const val SurfaceVariant = 0xE4ECE6
    const val OnSurfaceVariant = 0x33413B
    const val Outline = 0x63756C
    const val Focus = 0x134E4A
    const val Danger = 0xB3261E
    const val OnDanger = 0xFFFFFF

    const val CalmBackground = 0xF2F7F4
    const val CalmSurface = 0xFFFFFF

    const val DarkPrimary = 0x8FD3F4
    const val DarkOnPrimary = 0x003549
    const val DarkSecondary = 0x86D4A9
    const val DarkOnSecondary = 0x003822
    const val DarkBackground = 0x0F1412
    const val DarkSurface = 0x17201C
    const val DarkOnSurface = 0xE8F0EA
    const val DarkSurfaceVariant = 0x25342F
    const val DarkOnSurfaceVariant = 0xC9D4CD
    const val DarkOutline = 0x9AA79F
    const val DarkDanger = 0xFFB4AB
    const val DarkOnDanger = 0x690005

    const val HighContrastPrimary = 0x00324A
    const val HighContrastOnPrimary = 0xFFFFFF
    const val HighContrastSecondary = 0x003B2D
    const val HighContrastOnSecondary = 0xFFFFFF
    const val HighContrastBackground = 0xFFFFFF
    const val HighContrastOnBackground = 0x000000
    const val HighContrastSurfaceVariant = 0xF0F4F1
    const val HighContrastOutline = 0x000000
    const val HighContrastDanger = 0x8C0000

    fun contrastRatio(foreground: Int, background: Int): Double {
        val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
        val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(rgb: Int): Double {
        val r = linearChannel((rgb shr 16) and 0xFF)
        val g = linearChannel((rgb shr 8) and 0xFF)
        val b = linearChannel(rgb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linearChannel(channel: Int): Double {
        val normalized = channel / 255.0
        return if (normalized <= 0.03928) {
            normalized / 12.92
        } else {
            ((normalized + 0.055) / 1.055).pow(2.4)
        }
    }
}

internal fun colorFromRgb(rgb: Int): Color = Color(0xFF000000L or rgb.toLong())
