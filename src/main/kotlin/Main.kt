package io.paoloconte

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.ParseOptions
import notion.api.v1.NotionClient
import notion.api.v1.logging.Slf4jLogger
import notion.api.v1.model.blocks.ChildPageBlock
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.walk

private val logger = LoggerFactory.getLogger("Main")

@OptIn(ExperimentalPathApi::class)
fun main(args: Array<String>) {
    val notionToken = System.getenv("NOTION_TOKEN")
    val targetPage = System.getenv("TARGET_PAGE")
    val filesPath = Paths.get(args[0])

    NotionClient(token = notionToken, logger = Slf4jLogger()).use { notionClient ->
        filesPath.walk().filter { it.extension == "yaml" }.forEach { file ->
            createDocumentationPage(notionClient, targetPage, file)
        }
    }
}

private fun createDocumentationPage(
    client: NotionClient,
    targetPage: String,
    file: Path
) {
    logger.info("Processing ${file.fileName}")

    val options = ParseOptions().apply { isResolveFully = true }
    val swagger = OpenAPIParser().readLocation(file.absolutePathString(), null, options)

    if (swagger.openAPI.info == null) {
        logger.warn("Skipping ${file.fileName} because it does not have an info section")
        return
    }

    val pageTitle = swagger.openAPI.info.title
    if (pageTitle.isNullOrBlank()) {
        logger.warn("Please add a title to your OpenAPI specification")
        return
    }

    logger.info("Deleting Page $pageTitle if exists")
    deletePage(client, targetPage, pageTitle)
    logger.info("Creating Page $pageTitle")
    val page = createPage(client, targetPage, swagger.openAPI.info.title)
    logger.info("Created page $pageTitle with id ${page.id}")
    val template = NotionTemplate.render(swagger, file.fileName.toString())
    val blocks = client.appendBlockChildren(page.id, template)
    logger.info("Added ${blocks.results.size} blocks to page $pageTitle")
}

private fun createPage(client: NotionClient, targetPage: String, title: String): Page {
    return client.createPage(
        PageParent(pageId = targetPage), mapOf("title" to title(title))
    )
}

private fun deletePage(client: NotionClient, targetPage: String, pageTitle: String) {
    val children = client.retrieveBlockChildren(targetPage)
    children.results
        .filterIsInstance<ChildPageBlock>()
        .filter { it.childPage.title == pageTitle }
        .forEach {
            client.deleteBlock(it.id!!)
        }
}
