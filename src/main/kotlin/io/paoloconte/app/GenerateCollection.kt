package io.paoloconte.app

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*
import io.swagger.parser.OpenAPIParser
import io.swagger.util.Yaml
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.SpecVersion.V31
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.parser.core.models.ParseOptions
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.walk


object GenerateCollection {

    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    @JvmStatic
    fun generate(folders: List<String>, outPath: String) {
        logger.info("Generating collection")
        val collection = createCollection(folders)
        val yaml = generateYaml(collection)
        File(outPath).writeText(yaml)
        logger.info("Collection written to $outPath")
    }

    @OptIn(ExperimentalPathApi::class)
    private fun createCollection(folders: List<String>): OpenAPI {
        val collection = OpenAPI(V31).apply {
            openapi = "3.1.0"
            specVersion = V31
            info = Info().apply {
                version = "1.0.0"
                title = "Collection"
                description = "Collection of all the OpenAPI specifications"
            }
            paths = Paths()
            servers = listOf(
                Server().apply {
                    url = "{{base_url}}"
                }
            )
            components = Components()
            components.securitySchemes = mutableMapOf()
        }

        val yamlFiles = folders.flatMap { path ->
            Path.of(path).walk().filter { it.extension == "yaml" || it.extension == "yml" }
        }
        yamlFiles.forEach { file ->
            val options = ParseOptions().apply {
                isResolve = true
                isResolveFully = true
                isValidateExternalRefs = true
            }
            val swagger = OpenAPIParser().readLocation(file.absolutePathString(), null, options)
            swagger.openAPI.info ?: return@forEach
            swagger.openAPI.paths?.forEach { (path, pathItem) ->
                pathItem.servers = swagger.openAPI.servers?.filter { it.url != "/" }?.takeIf { it.isNotEmpty() }
                pathItem.readOperations().forEach { operation ->
                    operation.security = operation.security ?: swagger.openAPI.security
                    operation.requestBody?.content?.forEach { (mediaType, mediaTypeObject) ->
                        mediaTypeObject.schema?.let { schema ->
                            fixModel(schema)
                        }
                    }
                }
                collection.paths[path] = pathItem
            }
            collection.components.securitySchemes.putAll(swagger.openAPI.components.securitySchemes)
        }
        return collection
    }

    // improves compatibility with other tools
    private fun fixModel(schema: Schema<*>) {
        schema.properties?.forEach{ (key, value) ->
            value.type = value.type ?: value.types?.firstOrNull()
            fixModel(value)
        }
        schema.items?.let {
            fixModel(it)
        }
    }

    private fun generateYaml(collection: OpenAPI): String {
        val factory = (Yaml.mapper().factory as YAMLFactory)
        factory
            .disable(WRITE_DOC_START_MARKER)
            .enable(MINIMIZE_QUOTES)
            .enable(ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
        return Yaml.pretty().writeValueAsString(collection)
    }
}