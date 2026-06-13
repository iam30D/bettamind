package org.bettamind.shared.localization

import kotlinx.serialization.Serializable

@Serializable
data class LocaleTag(val value: String) {
    init {
        require(value.isNotBlank()) { "Locale tag cannot be blank." }
    }
}

object BettamindLocales {
    val source = LocaleTag("en")
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
