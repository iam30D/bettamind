package org.bettamind.shared.accessibility

import org.bettamind.shared.localization.BettamindLocales
import org.bettamind.shared.localization.LocaleTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlobalLocalizationAccessibilityTest {
    @Test
    fun localeProfilesCoverEveryInitialTargetWithRtlAndScriptFallbacks() {
        val profiles = BettamindLocaleAccessibilityCatalog.profiles()

        assertEquals(BettamindLocales.initialTargets.map { it.value }, profiles.map { it.locale.value })
        assertEquals(BettamindLayoutDirection.Rtl, BettamindLocaleAccessibilityCatalog.profileFor(LocaleTag("ar")).layoutDirection)
        assertEquals(BettamindScript.Arabic, BettamindLocaleAccessibilityCatalog.profileFor(LocaleTag("ar")).script)
        assertEquals(BettamindScript.Devanagari, BettamindLocaleAccessibilityCatalog.profileFor(LocaleTag("hi")).script)
        assertEquals(BettamindScript.HanSimplified, BettamindLocaleAccessibilityCatalog.profileFor(LocaleTag("zh-Hans")).script)
        assertTrue(profiles.all { it.fontFallbackKeys.isNotEmpty() })
    }

    @Test
    fun localizationReadinessRequiresCompleteKeysAndQualifiedSafetyReview() {
        val sourceKeys = setOf(
            "app_name",
            "privacy_lock_title",
            "harm_safety_privacy_note",
            "support_bridge_review_note",
            "offline_speech_microphone_body",
            "daily_checkin_title",
        )
        val completeLocalizedKeys = BettamindLocales.initialTargets.associateWith { sourceKeys }

        val draftReport = LocalizationReadinessPolicy.evaluate(
            sourceKeys = sourceKeys,
            localizedKeysByLocale = completeLocalizedKeys,
            reviewRecords = emptyList(),
        )

        assertTrue(draftReport.allResourceKeysPresent)
        assertFalse(draftReport.qualifiedSafetyReviewComplete)
        assertFalse(draftReport.productionReady)
        assertTrue("privacy_lock_title" in draftReport.safetyCriticalKeys)
        assertTrue("harm_safety_privacy_note" in draftReport.safetyCriticalKeys)
        assertTrue("offline_speech_microphone_body" in draftReport.safetyCriticalKeys)
        assertFalse("daily_checkin_title" in draftReport.safetyCriticalKeys)

        val reviewed = sourceKeys
            .filter { LocalizationReadinessPolicy.requiresQualifiedReview(LocalizationReadinessPolicy.surfaceForKey(it)) }
            .flatMap { key ->
                BettamindLocales.initialTargets.map { locale ->
                    TranslationReviewRecord(
                        stringKey = key,
                        surface = LocalizationReadinessPolicy.surfaceForKey(key),
                        locale = locale,
                        status = if (locale == BettamindLocales.source) {
                            TranslationReviewStatus.SourceReviewed
                        } else {
                            TranslationReviewStatus.QualifiedHumanReviewed
                        },
                    )
                }
            }
        val reviewedReport = LocalizationReadinessPolicy.evaluate(
            sourceKeys = sourceKeys,
            localizedKeysByLocale = completeLocalizedKeys,
            reviewRecords = reviewed,
        )

        assertTrue(reviewedReport.productionReady)
    }

    @Test
    fun localizationReadinessReportsMissingLocaleKeys() {
        val sourceKeys = setOf("app_name", "privacy_lock_title")
        val localizedKeys = BettamindLocales.initialTargets.associateWith { locale ->
            if (locale.value == "yo") setOf("app_name") else sourceKeys
        }

        val report = LocalizationReadinessPolicy.evaluate(
            sourceKeys = sourceKeys,
            localizedKeysByLocale = localizedKeys,
            reviewRecords = emptyList(),
        )

        assertFalse(report.allResourceKeysPresent)
        assertEquals(setOf("privacy_lock_title"), report.localeReports.single { it.locale.value == "yo" }.missingKeys)
    }

    @Test
    fun localeFormattingUsesLocaleAwareDateAndPluralRules() {
        assertEquals(DateOrder.MonthDayYear, LocaleFormattingPolicy.profileFor(LocaleTag("en")).dateOrder)
        assertEquals(DateOrder.DayMonthYear, LocaleFormattingPolicy.profileFor(LocaleTag("fr")).dateOrder)
        assertEquals(DateOrder.YearMonthDay, LocaleFormattingPolicy.profileFor(LocaleTag("zh-Hans")).dateOrder)
        assertEquals(PluralCategory.One, LocaleFormattingPolicy.pluralCategory(LocaleTag("en"), 1))
        assertEquals(PluralCategory.Other, LocaleFormattingPolicy.pluralCategory(LocaleTag("en"), 2))
        assertEquals(PluralCategory.Two, LocaleFormattingPolicy.pluralCategory(LocaleTag("ar"), 2))
        assertEquals(PluralCategory.Few, LocaleFormattingPolicy.pluralCategory(LocaleTag("ar"), 5))
        assertEquals(PluralCategory.Many, LocaleFormattingPolicy.pluralCategory(LocaleTag("ar"), 20))
    }

    @Test
    fun accessibilityPlanSupportsLargeTextReducedMotionLowLiteracyAndScreenReaders() {
        val plan = AccessibilityReadinessPolicy.planFor(
            AccessibilityPreferences(
                locale = LocaleTag("ar"),
                screenReaderOptimized = true,
                largeTextScale = 2.0f,
                reducedMotion = true,
                lowLiteracyMode = true,
                accessibleTypography = true,
            ),
        )

        assertEquals(BettamindLayoutDirection.Rtl, plan.layoutDirection)
        assertTrue(plan.supportsLargeText)
        assertEquals(MotionTreatment.StaticSteps, plan.timerMotionTreatment)
        assertEquals(MotionTreatment.StaticSteps, plan.groundingMotionTreatment)
        assertTrue(plan.simplifiedCopyRequired)
        assertTrue(LocalizedSurface.AppLock in plan.screenReaderLabelsRequired)
        assertTrue(LocalizedSurface.SupportBridge in plan.screenReaderLabelsRequired)
        assertTrue("atkinson_hyperlegible_regular" in plan.fontFallbackKeys)
        assertTrue("noto_sans_arabic_variable" in plan.fontFallbackKeys)
    }
}
