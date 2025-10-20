package io.paoloconte.app

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import io.paoloconte.config.Config
import io.paoloconte.notion.NotionAdapter
import io.paoloconte.template.NotionTemplate1
import io.paoloconte.template.NotionTemplate2
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.ParseOptions
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class App(
    private val notionToken: String,
    configFile: File,
) {

    private val config = Yaml.default.decodeFromStream<Config>(configFile.inputStream())
    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    @OptIn(ExperimentalPathApi::class)
    fun run() {
        NotionAdapter(token = notionToken).use { notionClient ->
            config.pages.forEach { targetPage ->
                val apiFolder = Path.of(targetPage.apiFolder)
                val apiFiles = apiFolder.walk().filter { it.extension in arrayOf("yaml", "yml")  }
                apiFiles.forEach { file ->
                    createDocumentationPage(notionClient, targetPage.notionPageId, file)
                }
            }
        }

        if (!config.generateCollection.isNullOrBlank()) {
            val folders = config.pages.map { it.apiFolder }
            GenerateCollection.generate(folders, config.generateCollection)
        }
    }

    private fun createDocumentationPage(
        client: NotionAdapter,
        targetPage: String,
        file: Path
    ) {
        val modified = file.getLastModifiedTime()
        logger.info("Processing ${file.fileName} $modified")

        val options = ParseOptions().apply {
            isResolve = true
            isResolveFully = false
            isValidateExternalRefs = true
        }
        val swagger = OpenAPIParser().readLocation(file.absolutePathString(), null, options)

        if (swagger.messages?.any { it.startsWith("Exception safe-checking yaml content") } == true) {
            error("Invalid YAML file: ${file.fileName}\n${swagger.messages.joinToString("\n")} ")
        }

        if (swagger.openAPI?.info == null) {
            logger.warn("Skipping ${file.fileName} because it does not have an info section")
            return
        }

        val pageTitle = swagger.openAPI.info.title
        if (pageTitle.isNullOrBlank()) {
            logger.warn("Please add a title to your OpenAPI specification")
            return
        }

        val flatten = swagger.openAPI.info.extensions?.get("x-notion-flatten") as? Boolean ?: false
        val showHeader = swagger.openAPI.info.extensions?.get("x-notion-header") as? Boolean ?: true
        val template = when (config.template) {
            2 -> NotionTemplate2(swagger, file.fileName.toString(), flatten, showHeader).render()
            else -> NotionTemplate1(swagger, file.fileName.toString(), flatten, showHeader).render()
        }


        logger.info("Preparing page '$pageTitle'")
        val pageId = client.getOrCreatePage(targetPage, swagger.openAPI.info.title)
        val generatedTime = client.getPageUpdatedDateTime(pageId)
        if (generatedTime != null && generatedTime.toInstant().epochSecond >= modified.toInstant().epochSecond) {
            logger.info("Page '$pageTitle' is up to date ($generatedTime)")
            return
        }

        logger.info("Page '$pageTitle' not up to date ($generatedTime) -> Updating")
        client.deletePageContents(pageId, pageTitle)
        logger.info("Writing template to page '$pageTitle'")
        val blocks = client.writeTemplate(pageId, template)
        logger.info("Added ${blocks.size} blocks to page '$pageTitle'")
        client.updatePage(pageId)
    }

}