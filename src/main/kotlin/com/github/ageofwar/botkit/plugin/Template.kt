package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.ktelegram.*
import com.github.ageofwar.ktelegram.text.toMarkdown
import freemarker.core.CommonMarkupOutputFormat
import freemarker.core.CommonTemplateMarkupOutputModel
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateException
import java.io.StringWriter
import java.io.Writer

private fun String.template(args: Map<String, Any?>, configuration: Configuration.() -> Unit): String {
    val reader = reader()
    val writer = StringWriter()
    val configuration = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).apply {
        registeredCustomOutputFormats = listOf(MarkdownOutputFormat, HtmlOutputFormat)
        configuration()
    }
    val template = Template("Botkit", reader, configuration)
    try {
        template.process(args, writer)
    } catch (e: TemplateException) {
        return this
    }
    return writer.toString()
}

fun String.template(args: Map<String, Any?>) = template(args) {  }
fun String.template(vararg args: Pair<String, Any?>) = template(args.toMap())

fun Text.Companion.parseMarkdown(text: String, args: Map<String, Any?>): Text = parseMarkdown(text.template(args) { outputFormat = MarkdownOutputFormat })
fun Text.Companion.parseMarkdown(text: String, vararg args: Pair<String, Any?>): Text = parseMarkdown(text, args.toMap())
fun Text.Companion.parseHtml(text: String, args: Map<String, Any?>): Text = parseHtml(text.template(args) { outputFormat = HtmlOutputFormat })
fun Text.Companion.parseHtml(text: String, vararg args: Pair<String, Any?>): Text = parseHtml(text, args.toMap())

fun Text.templateMarkdownText(args: Map<String, Any?>) = Text.parseMarkdown(text, args)
fun Text.templateMarkdownText(vararg args: Pair<String, Any?>) = Text.parseMarkdown(text, args.toMap())
fun Text.templateHtmlText(args: Map<String, Any?>) = Text.parseHtml(text, args)
fun Text.templateHtmlText(vararg args: Pair<String, Any?>) = Text.parseHtml(text, args.toMap())

fun MessageContent<*>.templateMarkdownText(args: Map<String, Any?>) = when(this) {
    is TextContent -> copy(text = text.templateMarkdownText(args))
    is PhotoContent -> copy(caption = caption?.templateMarkdownText(args))
    is VideoContent -> copy(caption = caption?.templateMarkdownText(args))
    is AudioContent -> copy(caption = caption?.templateMarkdownText(args))
    is VoiceContent -> copy(caption = caption?.templateMarkdownText(args))
    is AnimationContent -> copy(caption = caption?.templateMarkdownText(args))
    is DocumentContent -> copy(caption = caption?.templateMarkdownText(args))
    else -> this
}
fun MessageContent<*>.templateMarkdownText(vararg args: Pair<String, Any?>) = templateMarkdownText(args.toMap())

fun MessageContent<*>.templateHtmlText(args: Map<String, Any?>) = when(this) {
    is TextContent -> copy(text = text.templateHtmlText(args))
    is PhotoContent -> copy(caption = caption?.templateHtmlText(args))
    is VideoContent -> copy(caption = caption?.templateHtmlText(args))
    is AudioContent -> copy(caption = caption?.templateHtmlText(args))
    is VoiceContent -> copy(caption = caption?.templateHtmlText(args))
    is AnimationContent -> copy(caption = caption?.templateHtmlText(args))
    is DocumentContent -> copy(caption = caption?.templateHtmlText(args))
    else -> this
}
fun MessageContent<*>.templateHtmlText(vararg args: Pair<String, Any?>) = templateHtmlText(args.toMap())

@Deprecated("Use Text.Companion.parseMarkdown instead", ReplaceWith("Text.Companion.parseMarkdown"))
fun Text.template(args: Map<String, Any?>): Text {
    return Text.parseMarkdown(toMarkdown().template(args) { outputFormat = MarkdownOutputFormat })
}

@Deprecated("Use Text.Companion.parseMarkdown instead", ReplaceWith("Text.Companion.parseMarkdown"))
fun Text.template(vararg args: Pair<String, Any?>): Text {
    return template(args.toMap())
}

@Deprecated("Use Text.templateText instead", ReplaceWith("Text.templateText"))
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

@Deprecated("Use Text.templateText instead", ReplaceWith("Text.templateText"))
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
