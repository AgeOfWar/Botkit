package com.github.ageofwar.botkit.plugin

import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateException
import java.io.StringWriter

fun String.template(vararg args: Pair<String, Any?>): String {
    val reader = reader()
    val writer = StringWriter()
    val template = Template("Botkit", reader, Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS))
    try {
        template.process(args.toMap(), writer)
    } catch (e: TemplateException) {
        return this
    }
    return writer.toString()
}
