package org.bettamind.shared.localization

import kotlinx.serialization.Serializable

@Serializable
data class LocaleTag(val value: String) {
    init {
        require(value.isNotBlank()) { "Locale tag cannot be blank." }
    }

    val language: String
        get() = value.substringBefore('-').lowercase()

    val isRtl: Boolean
        get() = language in BettamindLocales.rtlLanguages
}

object BettamindLocales {
    val source = LocaleTag("en")
    val rtlValidationLocale = LocaleTag("ar")
    val rtlLanguages = setOf("ar")
    val initialTargets = listOf(
        LocaleTag("en"),
        LocaleTag("fr"),
        LocaleTag("es"),
        LocaleTag("pt"),
        LocaleTag("ar"),
        LocaleTag("hi"),
        LocaleTag("zh-Hans"),
        LocaleTag("ha"),
        LocaleTag("yo"),
        LocaleTag("ig"),
    )
}
