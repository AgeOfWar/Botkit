package com.github.ageofwar.botkit.plugin

import com.github.ageofwar.ktelegram.*
import com.github.ageofwar.ktelegram.text.parseMarkdown
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
    val configuration = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).apply(configuration)
    val template = Template("Botkit", reader, configuration)
    try {
        template.process(args, writer)
    } catch (e: TemplateException) {
        return this
    }
    return writer.toString()
}

fun String.template(args: Map<String, Any?>) = template(args) { registeredCustomOutputFormats = listOf(MarkdownOutputFormat) }

fun String.template(vararg args: Pair<String, Any?>) = template(args.toMap())

fun Text.template(args: Map<String, Any?>): Text {
    return Text.parseMarkdown(toMarkdown().template(args) {
        registeredCustomOutputFormats = listOf(MarkdownOutputFormat)
        outputFormat = MarkdownOutputFormat
    })
}

fun Text.template(vararg args: Pair<String, Any?>): Text {
    return template(args.toMap())
}

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

fun MessageContent<*>.template(vararg args: Pair<String, Any?>) = template(args.toMap())

private class TemplateMarkdownOutputModel(plainTextContent: String?, markupContent: String?) : CommonTemplateMarkupOutputModel<TemplateMarkdownOutputModel>(plainTextContent, markupContent) {
    override fun getOutputFormat() = MarkdownOutputFormat
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
