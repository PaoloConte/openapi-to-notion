package io.paoloconte.notion

import notion.api.v1.NotionClient
import notion.api.v1.exception.NotionAPIError
import notion.api.v1.http.JavaNetHttpClient
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

    companion object {
        private const val PROP_HASH = "OpenAPI-Hash"
        private val logger = LoggerFactory.getLogger(NotionAdapter::class.simpleName)
    }
    
    private val client = NotionClient(token = token, logger = Slf4jLogger(), httpClient = JavaNetHttpClient(connectTimeoutMillis = 60000, readTimeoutMillis = 60000))

    /** Prepares a page by creating it if it does not exist or deleting its contents if it does */
    fun getOrCreatePage(parentPage: String, pageTitle: String): String {
        val children = withRetry { client.retrieveBlockChildren(parentPage) }
        val pageId = children.results
            .filterIsInstance<ChildPageBlock>()
            .firstOrNull { it.childPage.title == pageTitle }
            ?.id

        if (pageId == null) {
            return createPage(parentPage, pageTitle).id
        }

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
                properties = mapOf("title" to titleProperty(title)),
                icon = Emoji(emoji = "\u2728")
            )
        }
    }

    fun updatePageProperties(pageId: String, hash: String) {
        logger.info("Updating Page '$pageId' with hash $hash")
        return withRetry {
            client.updatePage(
                pageId = pageId,
                properties = mapOf(PROP_HASH to textProperty(hash)),
            )
        }
    }

    fun getPageHash(pageId: String): String {
        val property = withRetry { client.retrievePagePropertyItem(pageId, PROP_HASH) }
        return property.richText?.text?.content?.trim()
            ?: property.results?.firstOrNull()?.richText?.text?.content?.trim()
            ?: ""
    }

    fun deletePageContents(pageId: String, pageTitle: String) {
        logger.info("Deleting contents of page '$pageTitle'")
        do {
            val children = withRetry { client.retrieveBlockChildren(pageId) }
            children.results.forEach { block ->
                logger.info("Deleting block ${block.id} from page $pageTitle")
                withRetry { client.deleteBlock(block.id!!) }
            }
        } while (children.hasMore)
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
            } catch (e: Exception) {
                logger.error("Request failed", e)
                throw e
            }
        }
        throw RuntimeException("Too many retries")
    }


}