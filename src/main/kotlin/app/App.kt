package io.paoloconte.app

import io.paoloconte.notion.NotionAdapter
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.ParseOptions
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.walk

class App(
    private val notionToken: String,
    private val targetPage: String,
    private val filesPath: Path
) {

    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    @OptIn(ExperimentalPathApi::class)
    fun run() {
        NotionAdapter(token = notionToken).use { notionClient ->
            val yamlFiles = filesPath.walk().filter { it.extension == "yaml" }
            yamlFiles.forEach { file ->
                createDocumentationPage(notionClient, targetPage, file)
            }
        }
    }

    private fun createDocumentationPage(
        client: NotionAdapter,
        targetPage: String,
        file: Path
    ) {
        logger.info("Processing ${file.fileName}")

        val options = ParseOptions().apply { isResolveFully = true }
        val swagger = OpenAPIParser().readLocation(file.absolutePathString(), null, options)

        if (swagger.openAPI?.info == null) {
            logger.warn("Skipping ${file.fileName} because it does not have an info section")
            return
        }

        val pageTitle = swagger.openAPI.info.title
        if (pageTitle.isNullOrBlank()) {
            logger.warn("Please add a title to your OpenAPI specification")
            return
        }

        val pageId = client.preparePage(targetPage, swagger.openAPI.info.title)
        val template = NotionTemplate.render(swagger, file.fileName.toString())
        logger.info("Writing template to page '$pageTitle'")
        val blocks = client.writeTemplate(pageId, template)
        logger.info("Added ${blocks.size} blocks to page '$pageTitle'")
    }

}