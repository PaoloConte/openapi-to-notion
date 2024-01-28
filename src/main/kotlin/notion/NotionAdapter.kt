package io.paoloconte.notion

import notion.api.v1.NotionClient
import notion.api.v1.exception.NotionAPIError
import notion.api.v1.logging.Slf4jLogger
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.blocks.ChildPageBlock
import notion.api.v1.model.common.Emoji
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import org.slf4j.LoggerFactory
import java.io.Closeable
import kotlin.math.min

class NotionAdapter(
    private val token: String,
): Closeable {

    private val logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val client = NotionClient(token = token, logger = Slf4jLogger())

    /** Prepares a page by creating it if it does not exist or deleting its contents if it does */
    fun preparePage(parentPage: String, pageTitle: String): String {
        val children = withRetry { client.retrieveBlockChildren(parentPage) }
        val pageId = children.results
            .filterIsInstance<ChildPageBlock>()
            .firstOrNull { it.childPage.title == pageTitle }
            ?.id

        if (pageId == null) {
            return createPage(parentPage, pageTitle).id
        }

        deletePageContents(pageId, pageTitle)
        return pageId
    }

    /** Writes a template to a page */
    fun writeTemplate(blockId: String, blocks: List<Block>): List<Block> {
        return blocks.chunked(100).flatMap { chunk ->
            val saved = withRetry {
                client.appendBlockChildren(blockId, chunk)
            }
            saved.results
        }
    }

    private fun createPage(targetPage: String, title: String): Page {
        logger.info("Creating Page '$title'")
        return withRetry {
            client.createPage(
                parent = PageParent(pageId = targetPage),
                properties = mapOf("title" to title(title)),
                icon = Emoji(emoji = "\u2728")
            )
        }
    }

    private fun deletePageContents(pageId: String, pageTitle: String) {
        logger.info("Deleting contents of page '$pageTitle'")
        val children = withRetry { client.retrieveBlockChildren(pageId) }
        children.results.forEach { block ->
                logger.info("Deleting block ${block.id} from page $pageTitle")
                withRetry { client.deleteBlock(block.id!!) }
            }
    }

    override fun close() {
        client.close()
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


}