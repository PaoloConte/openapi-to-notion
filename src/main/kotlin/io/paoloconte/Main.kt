package io.paoloconte

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import io.paoloconte.app.App
import java.io.File


fun main(args: Array<String>) = Cli().main(args)

private class Cli : CliktCommand() {
    val configFile: File? by option().file(mustExist = true, canBeDir = false).help("YAML Configuration file")

    override fun run() {
        val configFile = configFile ?: error("Missing configuration file")
        val notionToken = System.getenv("NOTION_TOKEN")
        App(notionToken, configFile).run()
    }
}
