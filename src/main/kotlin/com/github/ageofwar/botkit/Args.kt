package com.github.ageofwar.botkit

import java.io.File
import kotlin.system.exitProcess

data class Args(
    val overrideToken: String?
)

fun parseArgs(vararg args: String): Args {
    var overrideToken: String? = null
    args.forEach {
        when {
            it.startsWith("-token=") -> overrideToken = it.substring("-token=".length)
            it == "-h" || it == "-help" || it == "-?" -> {
                val programFile = File(Args::class.java.protectionDomain.codeSource.location.toURI())
                println("""
                    Usage: java -jar ${programFile.name} [options]
                    where options include:
                        -token=<token> overrides token in bot.json
                        -? -h -help prints this help message to the output stream
                """.trimIndent())
                exitProcess(0)
            }
        }
    }
    return Args(overrideToken)
}
