package org.bettamind.shared.foundation

import org.bettamind.shared.localization.BettamindLocales
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FoundationTest {
    @Test
    fun sourceLocaleIsEnglish() {
        assertEquals("en", BettamindLocales.source.value)
    }

    @Test
    fun initialLocalesIncludeRtlArabic() {
        assertTrue(BettamindLocales.initialTargets.any { it.value == "ar" })
    }
}
