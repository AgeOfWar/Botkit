package com.github.ageofwar.botkit

import java.io.File
import kotlin.system.exitProcess

data class Args(
    val overrideToken: String?,
    val loggerName: String,
    val workingDirectory: String
)

fun parseArgs(vararg args: String): Args {
    var overrideToken: String? = null
    var loggerName = "default"
    var workingDirectory = "."
    args.forEach {
        when {
            it.startsWith("-token=") -> overrideToken = it.substring("-token=".length)
            it.startsWith("-logger=") -> loggerName = it.substring("-logger=".length)
            it.startsWith("-dir=") -> workingDirectory = it.substring("-dir=".length)
            it == "-h" || it == "-help" || it == "-?" -> {
                val programFile = File(Args::class.java.protectionDomain.codeSource.location.toURI())
                println("""
                    Usage: java -jar ${programFile.name} [options]
                    where options include:
                        -token=<token> overrides token in bot.json
                        -logger=<logger name> selects the logger from the logger.json, default to "default"
                        -dir=<working directory> changes the working directory
                        -? -h -help prints this help message to the output stream
                """.trimIndent())
                exitProcess(0)
            }
        }
    }
    return Args(overrideToken, loggerName, workingDirectory)
}
