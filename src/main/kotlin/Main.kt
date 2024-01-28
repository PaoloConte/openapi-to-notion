package io.paoloconte

import io.paoloconte.app.App
import java.nio.file.Paths


fun main(args: Array<String>) {
    val notionToken = System.getenv("NOTION_TOKEN")
    val targetPage = System.getenv("TARGET_PAGE")
    val filesPath = Paths.get(args[0])

    App(notionToken, targetPage, filesPath).run()
}



