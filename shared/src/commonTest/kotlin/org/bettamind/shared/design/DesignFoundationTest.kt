package org.bettamind.shared.design

import kotlin.test.Test
import kotlin.test.assertTrue

class DesignFoundationTest {
    @Test
    fun primaryPaletteMeetsTextContrastMinimums() {
        assertContrast(BettamindColorTokens.OnPrimary, BettamindColorTokens.Primary)
        assertContrast(BettamindColorTokens.OnSecondary, BettamindColorTokens.Secondary)
        assertContrast(BettamindColorTokens.OnBackground, BettamindColorTokens.Background)
        assertContrast(BettamindColorTokens.OnSurface, BettamindColorTokens.Surface)
        assertContrast(BettamindColorTokens.OnSurfaceVariant, BettamindColorTokens.SurfaceVariant)
        assertContrast(BettamindColorTokens.OnDanger, BettamindColorTokens.Danger)
    }

    @Test
    fun alternateThemesMeetTextContrastMinimums() {
        assertContrast(BettamindColorTokens.DarkOnPrimary, BettamindColorTokens.DarkPrimary)
        assertContrast(BettamindColorTokens.DarkOnSecondary, BettamindColorTokens.DarkSecondary)
        assertContrast(BettamindColorTokens.DarkOnSurface, BettamindColorTokens.DarkBackground)
        assertContrast(BettamindColorTokens.DarkOnSurfaceVariant, BettamindColorTokens.DarkSurfaceVariant)
        assertContrast(BettamindColorTokens.DarkOnDanger, BettamindColorTokens.DarkDanger)
        assertContrast(BettamindColorTokens.HighContrastOnPrimary, BettamindColorTokens.HighContrastPrimary)
        assertContrast(BettamindColorTokens.HighContrastOnBackground, BettamindColorTokens.HighContrastBackground)
        assertContrast(BettamindColorTokens.HighContrastOnBackground, BettamindColorTokens.HighContrastSurfaceVariant)
    }

    private fun assertContrast(foreground: Int, background: Int) {
        val ratio = BettamindColorTokens.contrastRatio(foreground, background)
        assertTrue(ratio >= 4.5, "Expected contrast ratio >= 4.5, got $ratio")
    }
}
