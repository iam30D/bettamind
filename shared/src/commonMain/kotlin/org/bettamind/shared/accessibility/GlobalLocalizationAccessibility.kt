package org.bettamind.shared.accessibility

import org.bettamind.shared.localization.BettamindLocales
import org.bettamind.shared.localization.LocaleTag

enum class BettamindLayoutDirection {
    Ltr,
    Rtl,
}

enum class BettamindScript {
    Latin,
    Arabic,
    Devanagari,
    HanSimplified,
}

data class LocaleAccessibilityProfile(
    val locale: LocaleTag,
    val layoutDirection: BettamindLayoutDirection,
    val script: BettamindScript,
    val fontFallbackKeys: List<String>,
) {
    init {
        require(fontFallbackKeys.isNotEmpty()) { "A locale profile needs at least one font fallback." }
    }
}

object BettamindLocaleAccessibilityCatalog {
    fun profiles(): List<LocaleAccessibilityProfile> =
        BettamindLocales.initialTargets.map(::profileFor)

    fun profileFor(locale: LocaleTag): LocaleAccessibilityProfile {
        val language = locale.language
        return LocaleAccessibilityProfile(
            locale = locale,
            layoutDirection = if (locale.isRtl) BettamindLayoutDirection.Rtl else BettamindLayoutDirection.Ltr,
            script = when (language) {
                "ar" -> BettamindScript.Arabic
                "hi" -> BettamindScript.Devanagari
                "zh" -> BettamindScript.HanSimplified
                else -> BettamindScript.Latin
            },
            fontFallbackKeys = when (language) {
                "ar" -> listOf("noto_sans_arabic_variable", "noto_sans_variable")
                "hi" -> listOf("noto_sans_devanagari_variable", "noto_sans_variable")
                "zh" -> listOf("noto_sans_sc_variable", "noto_sans_variable")
                else -> listOf("noto_sans_variable", "atkinson_hyperlegible_regular")
            },
        )
    }
}

enum class LocalizedSurface {
    AppLock,
    RelationalBoundary,
    HarmSafety,
    CrisisEmergency,
    LegalConsentPrivacy,
    DailyTool,
    Reminder,
    Calendar,
    LocalTrend,
    Worksheet,
    Timer,
    SupportBridge,
    ExportSync,
    OfflineSpeech,
    ReleaseReadiness,
    General,
}

enum class TranslationReviewStatus {
    SourceReviewed,
    DraftComplete,
    NeedsQualifiedHumanReview,
    QualifiedHumanReviewed,
}

data class TranslationReviewRecord(
    val stringKey: String,
    val surface: LocalizedSurface,
    val locale: LocaleTag,
    val status: TranslationReviewStatus,
) {
    init {
        require(stringKey.isNotBlank()) { "Translation review records need a string key." }
    }
}

data class LocaleCompletenessReport(
    val locale: LocaleTag,
    val missingKeys: Set<String>,
) {
    val complete: Boolean
        get() = missingKeys.isEmpty()
}

data class LocalizationReadinessReport(
    val localeReports: List<LocaleCompletenessReport>,
    val safetyCriticalKeys: Set<String>,
    val missingQualifiedReviewRecords: List<TranslationReviewRecord>,
) {
    val allResourceKeysPresent: Boolean
        get() = localeReports.all { it.complete }

    val qualifiedSafetyReviewComplete: Boolean
        get() = missingQualifiedReviewRecords.isEmpty()

    val productionReady: Boolean
        get() = allResourceKeysPresent && qualifiedSafetyReviewComplete
}

object LocalizationReadinessPolicy {
    fun surfaceForKey(key: String): LocalizedSurface =
        when {
            key.startsWith("privacy_lock") -> LocalizedSurface.AppLock
            key.startsWith("relational_boundary") -> LocalizedSurface.RelationalBoundary
            key.startsWith("harm_safety") -> LocalizedSurface.HarmSafety
            key.startsWith("compassionate_safety") -> LocalizedSurface.CrisisEmergency
            key.startsWith("support_bridge") -> LocalizedSurface.SupportBridge
            key.startsWith("daily_reminder") -> LocalizedSurface.Reminder
            key.startsWith("daily_calendar") -> LocalizedSurface.Calendar
            key.startsWith("daily_worksheet") -> LocalizedSurface.Worksheet
            key.startsWith("daily_breathing") ||
                key.startsWith("daily_grounding") -> LocalizedSurface.Timer
            key.startsWith("daily") -> LocalizedSurface.DailyTool
            key.startsWith("growth_age") -> LocalizedSurface.LegalConsentPrivacy
            key.startsWith("encrypted_sync") -> LocalizedSurface.ExportSync
            key.startsWith("offline_speech") -> LocalizedSurface.OfflineSpeech
            key.startsWith("release_readiness") -> LocalizedSurface.ReleaseReadiness
            else -> LocalizedSurface.General
        }

    fun requiresQualifiedReview(surface: LocalizedSurface): Boolean =
        surface in setOf(
            LocalizedSurface.AppLock,
            LocalizedSurface.RelationalBoundary,
            LocalizedSurface.HarmSafety,
            LocalizedSurface.CrisisEmergency,
            LocalizedSurface.LegalConsentPrivacy,
            LocalizedSurface.SupportBridge,
            LocalizedSurface.ExportSync,
            LocalizedSurface.OfflineSpeech,
            LocalizedSurface.ReleaseReadiness,
        )

    fun evaluate(
        sourceKeys: Set<String>,
        localizedKeysByLocale: Map<LocaleTag, Set<String>>,
        reviewRecords: List<TranslationReviewRecord>,
    ): LocalizationReadinessReport {
        val reports = BettamindLocales.initialTargets.map { locale ->
            LocaleCompletenessReport(
                locale = locale,
                missingKeys = sourceKeys - localizedKeysByLocale.orEmpty(locale),
            )
        }
        val safetyKeys = sourceKeys.filter { requiresQualifiedReview(surfaceForKey(it)) }.toSet()
        val reviewedPairs = reviewRecords
            .filter { it.status == TranslationReviewStatus.QualifiedHumanReviewed || it.status == TranslationReviewStatus.SourceReviewed }
            .map { it.locale.value to it.stringKey }
            .toSet()
        val missingReviews = BettamindLocales.initialTargets
            .flatMap { locale ->
                safetyKeys.mapNotNull { key ->
                    val requiredStatus = if (locale == BettamindLocales.source) {
                        TranslationReviewStatus.SourceReviewed
                    } else {
                        TranslationReviewStatus.QualifiedHumanReviewed
                    }
                    val pair = locale.value to key
                    if (pair in reviewedPairs) {
                        null
                    } else {
                        TranslationReviewRecord(
                            stringKey = key,
                            surface = surfaceForKey(key),
                            locale = locale,
                            status = requiredStatus,
                        )
                    }
                }
            }
        return LocalizationReadinessReport(
            localeReports = reports,
            safetyCriticalKeys = safetyKeys,
            missingQualifiedReviewRecords = missingReviews,
        )
    }

    private fun Map<LocaleTag, Set<String>>.orEmpty(locale: LocaleTag): Set<String> =
        entries.firstOrNull { it.key.value == locale.value }?.value ?: emptySet()
}

enum class DateOrder {
    MonthDayYear,
    DayMonthYear,
    YearMonthDay,
}

enum class PluralCategory {
    One,
    Two,
    Few,
    Many,
    Other,
}

data class LocaleFormattingProfile(
    val locale: LocaleTag,
    val dateOrder: DateOrder,
    val decimalSeparator: Char,
    val groupingSeparator: Char,
)

object LocaleFormattingPolicy {
    fun profileFor(locale: LocaleTag): LocaleFormattingProfile =
        when (locale.language) {
            "en" -> LocaleFormattingProfile(locale, DateOrder.MonthDayYear, '.', ',')
            "zh" -> LocaleFormattingProfile(locale, DateOrder.YearMonthDay, '.', ',')
            else -> LocaleFormattingProfile(locale, DateOrder.DayMonthYear, ',', '.')
        }

    fun pluralCategory(
        locale: LocaleTag,
        quantity: Int,
    ): PluralCategory =
        when (locale.language) {
            "ar" -> when {
                quantity == 1 -> PluralCategory.One
                quantity == 2 -> PluralCategory.Two
                quantity in 3..10 -> PluralCategory.Few
                quantity in 11..99 -> PluralCategory.Many
                else -> PluralCategory.Other
            }

            else -> if (quantity == 1) PluralCategory.One else PluralCategory.Other
        }
}

data class AccessibilityPreferences(
    val locale: LocaleTag = BettamindLocales.source,
    val screenReaderOptimized: Boolean = true,
    val largeTextScale: Float = 1.0f,
    val reducedMotion: Boolean = false,
    val lowLiteracyMode: Boolean = false,
    val accessibleTypography: Boolean = false,
) {
    init {
        require(largeTextScale in 1.0f..2.0f) {
            "Large text scale must stay within the validated range."
        }
    }
}

enum class MotionTreatment {
    Normal,
    StaticSteps,
}

data class AccessibilitySupportPlan(
    val layoutDirection: BettamindLayoutDirection,
    val fontFallbackKeys: List<String>,
    val minimumTouchTargetDp: Int,
    val timerMotionTreatment: MotionTreatment,
    val groundingMotionTreatment: MotionTreatment,
    val simplifiedCopyRequired: Boolean,
    val screenReaderLabelsRequired: Set<LocalizedSurface>,
    val largeTextScale: Float,
) {
    val supportsLargeText: Boolean
        get() = largeTextScale <= 2.0f && minimumTouchTargetDp >= 48
}

object AccessibilityReadinessPolicy {
    fun planFor(preferences: AccessibilityPreferences): AccessibilitySupportPlan {
        val profile = BettamindLocaleAccessibilityCatalog.profileFor(preferences.locale)
        val motion = if (preferences.reducedMotion) MotionTreatment.StaticSteps else MotionTreatment.Normal
        return AccessibilitySupportPlan(
            layoutDirection = profile.layoutDirection,
            fontFallbackKeys = if (preferences.accessibleTypography) {
                listOf("atkinson_hyperlegible_regular") + profile.fontFallbackKeys
            } else {
                profile.fontFallbackKeys
            }.distinct(),
            minimumTouchTargetDp = 48,
            timerMotionTreatment = motion,
            groundingMotionTreatment = motion,
            simplifiedCopyRequired = preferences.lowLiteracyMode,
            screenReaderLabelsRequired = if (preferences.screenReaderOptimized) {
                RequiredScreenReaderSurfaces
            } else {
                emptySet()
            },
            largeTextScale = preferences.largeTextScale,
        )
    }

    private val RequiredScreenReaderSurfaces = setOf(
        LocalizedSurface.AppLock,
        LocalizedSurface.DailyTool,
        LocalizedSurface.Reminder,
        LocalizedSurface.Calendar,
        LocalizedSurface.Timer,
        LocalizedSurface.SupportBridge,
        LocalizedSurface.HarmSafety,
        LocalizedSurface.ExportSync,
        LocalizedSurface.ReleaseReadiness,
    )
}
