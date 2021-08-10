package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.ktelegram.*
import com.github.ageofwar.ktelegram.text.parseHtml
import com.github.ageofwar.ktelegram.text.parseMarkdown
import com.github.ageofwar.ktelegram.text.toMarkdown
import freemarker.core.CommonMarkupOutputFormat
import freemarker.core.CommonTemplateMarkupOutputModel
import freemarker.template.*
import java.io.StringWriter
import java.io.Writer
import java.time.*
import java.util.*
import kotlin.random.Random


private fun String.template(args: Map<String, Any?>, configuration: Configuration.() -> Unit): String {
    val reader = reader()
    val writer = StringWriter()
    val configuration = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).apply {
        whitespaceStripping = false
        registeredCustomOutputFormats = listOf(MarkdownOutputFormat, HtmlOutputFormat)
        logTemplateExceptions = false
        objectWrapper = ObjectWrapper
        outputEncoding = "utf8"
        setSharedVariable("unicode", UnicodeTemplateMethodModel)
        setSharedVariable("random", Random)
        configuration()
    }
    val template = Template("Botkit", reader, configuration)
    template.process(args, writer)
    return writer.toString()
}

@JvmOverloads
fun String.template(args: Map<String, Any?>, locale: Locale = Locale.ROOT) = template(args) {
    this.locale = locale
}

@JvmOverloads
fun String.template(vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT) = template(args.toMap(), locale)

@JvmOverloads
fun Text.Companion.templateMarkdown(text: String, args: Map<String, Any?>, locale: Locale = Locale.ROOT): Text = parseMarkdown(text.template(args) {
    this.locale = locale
    outputFormat = MarkdownOutputFormat
})

@JvmOverloads
fun Text.Companion.templateMarkdown(text: String, vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT): Text = templateMarkdown(text, args.toMap(), locale)

@JvmOverloads
fun Text.Companion.templateHtml(text: String, args: Map<String, Any?>, locale: Locale = Locale.ROOT): Text = parseHtml(text.template(args) {
    this.locale = locale
    outputFormat = HtmlOutputFormat
})

@JvmOverloads
fun Text.Companion.templateHtml(text: String, vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT): Text = templateHtml(text, args.toMap(), locale)

@JvmOverloads
fun Text.templateMarkdownText(args: Map<String, Any?>, locale: Locale = Locale.ROOT) = Text.templateMarkdown(text, args, locale)

@JvmOverloads
fun Text.templateMarkdownText(vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT) = Text.templateMarkdown(text, args.toMap(), locale)

@JvmOverloads
fun Text.templateHtmlText(args: Map<String, Any?>, locale: Locale = Locale.ROOT) = Text.templateHtml(text, args, locale)

@JvmOverloads
fun Text.templateHtmlText(vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT) = Text.templateHtml(text, args.toMap(), locale)

@JvmOverloads
fun MessageContent<*>.templateMarkdownText(args: Map<String, Any?>, locale: Locale = Locale.ROOT) = when(this) {
    is TextContent -> copy(text = text.templateMarkdownText(args, locale))
    is PhotoContent -> copy(caption = caption?.templateMarkdownText(args, locale))
    is VideoContent -> copy(caption = caption?.templateMarkdownText(args, locale))
    is AudioContent -> copy(caption = caption?.templateMarkdownText(args, locale))
    is VoiceContent -> copy(caption = caption?.templateMarkdownText(args, locale))
    is AnimationContent -> copy(caption = caption?.templateMarkdownText(args, locale))
    is DocumentContent -> copy(caption = caption?.templateMarkdownText(args, locale))
    else -> this
}

@JvmOverloads
fun MessageContent<*>.templateMarkdownText(vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT) = templateMarkdownText(args.toMap(), locale)

@JvmOverloads
fun MessageContent<*>.templateHtmlText(args: Map<String, Any?>, locale: Locale = Locale.ROOT) = when(this) {
    is TextContent -> copy(text = text.templateHtmlText(args, locale))
    is PhotoContent -> copy(caption = caption?.templateHtmlText(args, locale))
    is VideoContent -> copy(caption = caption?.templateHtmlText(args, locale))
    is AudioContent -> copy(caption = caption?.templateHtmlText(args, locale))
    is VoiceContent -> copy(caption = caption?.templateHtmlText(args, locale))
    is AnimationContent -> copy(caption = caption?.templateHtmlText(args, locale))
    is DocumentContent -> copy(caption = caption?.templateHtmlText(args, locale))
    else -> this
}

@JvmOverloads
fun MessageContent<*>.templateHtmlText(vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT) = templateHtmlText(args.toMap(), locale)

@JvmOverloads
fun InlineMessageContent.templateMarkdownText(args: Map<String, Any?>, locale: Locale = Locale.ROOT) = when(this) {
    is InlineTextContent -> copy(text = text.templateMarkdownText(args, locale))
    is InlineVenueContent -> copy(
        venue = venue.copy(
            title = venue.title.template(args, locale),
            address = venue.address.template(args, locale),
            foursquareId = venue.foursquareId?.template(args, locale),
            foursquareType = venue.foursquareType?.template(args, locale),
            googlePlaceId = venue.googlePlaceId?.template(args, locale),
            googlePlaceType = venue.googlePlaceType?.template(args, locale)
        )
    )
    is InlineContactContent -> copy(
        phoneNumber = phoneNumber.template(args, locale),
        firstName = phoneNumber.template(args, locale),
        lastName = lastName?.template(args, locale),
        vcard = vcard?.template(args, locale)
    )
    is InlineInvoiceContent -> copy(
        title = title.template(args, locale),
        description = description.template(args, locale),
        startParameter = startParameter?.template(args, locale),
        providerData = providerData?.template(args, locale),
        photoUrl = photoUrl?.template(args, locale),
        providerToken = providerToken.template(args, locale)
    )
    else -> this
}

@JvmOverloads
fun InlineMessageContent.templateMarkdownText(vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT) = templateMarkdownText(args.toMap(), locale)

@JvmOverloads
fun InlineMessageContent.templateHtmlText(args: Map<String, Any?>, locale: Locale = Locale.ROOT) = when(this) {
    is InlineTextContent -> copy(text = text.templateHtmlText(args, locale))
    is InlineVenueContent -> copy(
        venue = venue.copy(
            title = venue.title.template(args, locale),
            address = venue.address.template(args, locale),
            foursquareId = venue.foursquareId?.template(args, locale),
            foursquareType = venue.foursquareType?.template(args, locale),
            googlePlaceId = venue.googlePlaceId?.template(args, locale),
            googlePlaceType = venue.googlePlaceType?.template(args, locale)
        )
    )
    is InlineContactContent -> copy(
        phoneNumber = phoneNumber.template(args, locale),
        firstName = phoneNumber.template(args, locale),
        lastName = lastName?.template(args, locale),
        vcard = vcard?.template(args, locale)
    )
    is InlineInvoiceContent -> copy(
        title = title.template(args, locale),
        description = description.template(args, locale),
        startParameter = startParameter?.template(args, locale),
        providerData = providerData?.template(args, locale),
        photoUrl = photoUrl?.template(args, locale),
        providerToken = providerToken.template(args, locale)
    )
    else -> this
}

@JvmOverloads
fun InlineMessageContent.templateHtmlText(vararg args: Pair<String, Any?>, locale: Locale = Locale.ROOT) = templateHtmlText(args.toMap(), locale)

@Deprecated("Use Text.Companion.templateMarkdown instead", ReplaceWith("Text.Companion.templateMarkdown"))
fun Text.template(args: Map<String, Any?>): Text {
    return Text.parseMarkdown(toMarkdown().template(args) { outputFormat = MarkdownOutputFormat })
}

@Deprecated("Use Text.Companion.templateMarkdown instead", ReplaceWith("Text.Companion.templateMarkdown"))
fun Text.template(vararg args: Pair<String, Any?>): Text {
    return template(args.toMap())
}

@Deprecated("Use MessageContent.templateMarkdownText instead", ReplaceWith("MessageContent.templateMarkdownText"))
fun MessageContent<*>.template(args: Map<String, Any?>) = when(this) {
    is TextContent -> copy(text = text.template(args))
    is PhotoContent -> copy(caption = caption?.template(args))
    is VideoContent -> copy(caption = caption?.template(args))
    is AudioContent -> copy(caption = caption?.template(args))
    is VoiceContent -> copy(caption = caption?.template(args))
    is AnimationContent -> copy(caption = caption?.template(args))
    is DocumentContent -> copy(caption = caption?.template(args))
    else -> this
}

@Deprecated("Use MessageContent.templateMarkdownText instead", ReplaceWith("MessageContent.templateMarkdownText"))
fun MessageContent<*>.template(vararg args: Pair<String, Any?>) = template(args.toMap())

private class TemplateMarkdownOutputModel(plainTextContent: String?, markupContent: String?) : CommonTemplateMarkupOutputModel<TemplateMarkdownOutputModel>(plainTextContent, markupContent) {
    override fun getOutputFormat() = MarkdownOutputFormat
}

private class TemplateHtmlOutputModel(plainTextContent: String?, markupContent: String?) : CommonTemplateMarkupOutputModel<TemplateHtmlOutputModel>(plainTextContent, markupContent) {
    override fun getOutputFormat() = HtmlOutputFormat
}

private object MarkdownOutputFormat : CommonMarkupOutputFormat<TemplateMarkdownOutputModel>() {
    override fun getName() = "Markdown"
    override fun getMimeType() = null
    override fun output(textToEsc: String, out: Writer) {
        textToEsc.forEach {
            when (it) {
                '\\', '*', '_', '~', '`', '[', ']', '(', ')' -> out.write("\\$it")
                else -> out.write(it.code)
            }
        }
    }
    override fun escapePlainText(plainTextContent: String) = buildString {
        plainTextContent.forEach {
            when (it) {
                '\\', '*', '_', '~', '`', '[', ']', '(', ')' -> append("\\$it")
                else -> append(it)
            }
        }
    }
    override fun isLegacyBuiltInBypassed(builtInName: String) = false
    override fun newTemplateMarkupOutputModel(plainTextContent: String?, markupContent: String?) = TemplateMarkdownOutputModel(plainTextContent, markupContent)
}

private object HtmlOutputFormat : CommonMarkupOutputFormat<TemplateHtmlOutputModel>() {
    override fun getName() = "HTML"
    override fun getMimeType() = null
    override fun output(textToEsc: String, out: Writer) {
        textToEsc.forEach {
            when (it) {
                '<' -> out.write("&lt;")
                '>' -> out.write("&gt;")
                '&' -> out.write("&amp;")
                else -> out.write(it.code)
            }
        }
    }
    override fun escapePlainText(plainTextContent: String) = buildString {
        plainTextContent.forEach {
            when (it) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '&' -> append("&amp;")
                else -> append(it)
            }
        }
    }
    override fun isLegacyBuiltInBypassed(builtInName: String) = false
    override fun newTemplateMarkupOutputModel(plainTextContent: String?, markupContent: String?) = TemplateHtmlOutputModel(plainTextContent, markupContent)
}

private object UnicodeTemplateMethodModel : TemplateMethodModelEx {
    override fun exec(args: MutableList<Any?>): Any {
        if (args.size != 1) throw TemplateModelException("Wrong arguments")
        return SimpleScalar((args[0] as SimpleNumber).asNumber.toChar().toString())
    }
}

private object ObjectWrapper : DefaultObjectWrapper(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS) {
    override fun wrap(obj: Any?): TemplateModel? = when (obj) {
        is Int -> SimpleNumber(obj)
        is Long -> SimpleNumber(obj)
        is Byte -> SimpleNumber(obj)
        is Short -> SimpleNumber(obj)
        is Double -> SimpleNumber(obj)
        is Float -> SimpleNumber(obj)
        is LocalDateTime -> SimpleDate(Date.from(obj.toInstant(ZoneOffset.UTC)), TemplateDateModel.DATETIME)
        is LocalDate -> SimpleDate(Date.from(obj.atStartOfDay().toInstant(ZoneOffset.UTC)), TemplateDateModel.DATE)
        is LocalTime -> SimpleDate(Date.from(obj.atDate(LocalDate.EPOCH).toInstant(ZoneOffset.UTC)), TemplateDateModel.TIME)
        is Instant -> SimpleDate(Date.from(obj), TemplateDateModel.DATETIME)
        is ZonedDateTime -> SimpleDate(Date.from(obj.toInstant()), TemplateDateModel.DATETIME)
        is OffsetDateTime -> SimpleDate(Date.from(obj.toInstant()), TemplateDateModel.DATETIME)
        is OffsetTime -> SimpleDate(Date.from(obj.atDate(LocalDate.EPOCH).toInstant()), TemplateDateModel.TIME)
        else -> super.handleUnknownType(obj)
    }
}
