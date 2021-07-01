package com.github.ageofwar.botkit.plugin

import java.util.*

fun <T> Map<String, T>.localized(locale: Locale) = get(locale.language) ?: get("")
fun <T> Map<String, T>.localized(languageCode: String) = localized(Locale.forLanguageTag(languageCode))
