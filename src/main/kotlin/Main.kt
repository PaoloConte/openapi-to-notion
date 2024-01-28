package io.paoloconte

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.ParseOptions
import notion.api.v1.NotionClient
import notion.api.v1.exception.NotionAPIError
import notion.api.v1.logging.Slf4jLogger
import notion.api.v1.model.blocks.ChildPageBlock
import notion.api.v1.model.common.Emoji
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.walk
import kotlin.math.min

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

    val pageId = preparePage(client, targetPage, swagger.openAPI.info.title)
    val template = NotionTemplate.render(swagger, file.fileName.toString())
    logger.info("Writing template to page '$pageTitle'")
    val blocks = withRetry { client.appendBlockChildren(pageId, template) }
    logger.info("Added ${blocks.results.size} blocks to page '$pageTitle'")
}

/** Prepares a page by creating it if it does not exist or deleting its contents if it does */
private fun preparePage(client: NotionClient, targetPage: String, pageTitle: String): String {
    val children = withRetry { client.retrieveBlockChildren(targetPage) }
    val pageId = children.results
        .filterIsInstance<ChildPageBlock>()
        .firstOrNull { it.childPage.title == pageTitle }
        ?.id

    if (pageId == null) {
        return createPage(client, targetPage, pageTitle).id
    }

    deletePageContents(client, pageId, pageTitle)
    return pageId
}

private fun createPage(client: NotionClient, targetPage: String, title: String): Page {
    logger.info("Creating Page '$title'")
    return withRetry {
        client.createPage(
            parent = PageParent(pageId = targetPage),
            properties = mapOf("title" to title(title)),
            icon = Emoji(emoji = "\u2728")
        )
    }
}

private fun deletePageContents(client: NotionClient, pageId: String, pageTitle: String) {
    logger.info("Deleting contents of page '$pageTitle'")
    val children = withRetry { client.retrieveBlockChildren(pageId) }
    children.results
        .forEach { block ->
            logger.info("Deleting block ${block.id} from page $pageTitle")
            withRetry { client.deleteBlock(block.id!!) }
        }
}

private fun <T> withRetry(maxTries: Int = 20, block: () -> T): T {
    var tries = 0
    var backoff = 1L
    while (tries++ < maxTries) {
        try {
            return block()
        } catch (e: NotionAPIError) {
            val status = e.httpResponse.status
            if (status == 429 || status >= 500) {
                backoff = min(2 * backoff, 10)
                val seconds = backoff
                logger.warn("Received status=$status, waiting for $seconds seconds")
                Thread.sleep(1000 * seconds)
            } else {
                logger.error("Request failed", e)
                throw e
            }
        }
    }
    throw RuntimeException("Too many retries")
}
