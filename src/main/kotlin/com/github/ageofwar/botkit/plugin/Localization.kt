package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.ktelegram.Sender
import com.github.ageofwar.ktelegram.User
import java.util.*

fun <T> Map<String, T>.localizedOrElse(locale: Locale?, orElse: String = "en"): T? = if (locale?.language.isNullOrEmpty()) get(orElse) else get(locale?.language!!) ?: get(orElse)
fun <T> Map<String, T>.localizedOrElse(languageCode: String?, orElse: String = "en"): T? = if (languageCode.isNullOrEmpty()) get(orElse) else localizedOrElse(Locale.forLanguageTag(languageCode), orElse)
fun <T> Map<String, T>.localizedOrElse(user: User?, orElse: String = "en"): T? = localizedOrElse(user?.languageCode, orElse)
fun <T> Map<String, T>.localizedOrElse(sender: Sender?, orElse: String = "en"): T? = localizedOrElse(sender as? User, orElse)

fun <T> Map<String, T>.localized(locale: Locale?, orElse: String = "en", lazyMessage: () -> Any? = { "Missing default locale '$orElse'" }): T = localizedOrElse(locale, orElse) ?: throw PluginException(lazyMessage().toString())
fun <T> Map<String, T>.localized(languageCode: String?, orElse: String = "en", lazyMessage: () -> Any? = { "Missing default locale '$orElse'" }): T = localizedOrElse(languageCode, orElse) ?: throw PluginException(lazyMessage().toString())
fun <T> Map<String, T>.localized(user: User?, orElse: String = "en", lazyMessage: () -> Any? = { "Missing default locale '$orElse'" }): T = localizedOrElse(user, orElse) ?: throw PluginException(lazyMessage().toString())
fun <T> Map<String, T>.localized(sender: Sender?, orElse: String = "en", lazyMessage: () -> Any? = { "Missing default locale '$orElse'" }): T = localizedOrElse(sender, orElse) ?: throw PluginException(lazyMessage().toString())

val User.locale: Locale get() = if (languageCode != null) Locale.forLanguageTag(languageCode) else Locale.ROOT
val Sender.locale: Locale get() = (this as? User)?.locale ?: Locale.ROOT

val Sender.languageCode: String? get() = (this as? User)?.languageCode
