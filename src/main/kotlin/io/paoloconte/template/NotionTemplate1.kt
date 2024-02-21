package io.paoloconte.template

import io.paoloconte.notion.BlocksBuilder
import io.paoloconte.notion.blocks
import io.paoloconte.notion.richText
import io.paoloconte.openapi.resolveSchema
import io.paoloconte.template.components.exampleItem
import io.paoloconte.template.components.pageHeader
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.parser.core.models.SwaggerParseResult
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.common.BlockColor
import notion.api.v1.model.common.RichTextColor.*

class NotionTemplate1(
    private val swagger: SwaggerParseResult,
    private val fileName: String,
    private val flatten: Boolean,
    private val showHeader: Boolean,
) : Template {

    private val consumedComponents = mutableListOf<Schema<*>>()

    override fun render(): List<Block> = blocks {
        // wrap everything inside a paragraph, so it can be easily deleted with few calls

        paragraph("") {
            pageHeader(fileName, showHeader)
        }

        paragraph("") {
            summarySection()
        }

        for (path in swagger.openAPI.paths) {
            for ((method, operation) in path.value.readOperationsMap()) {
                operationSection(path.key, method.name, operation)
            }
        }

        val components = swagger.openAPI.components.schemas
            ?.filter { (_, schema) -> !consumedComponents.contains(schema) }
            ?: emptyMap()
        if (!flatten && components.isNotEmpty()) {
            paragraph("") {
                componentsSection(components)
            }
        }

        paragraph("") {
            authenticationSection()
        }

    }

    private fun BlocksBuilder.summarySection() {
        heading1("Summary")

        table(4, hasColumnHeader = true) {
            row {
                cell(richText("Method"))
                cell(richText("Endpoint"))
                cell(richText("Authentication"))
                cell(richText("Description"))
            }

            for (path in swagger.openAPI.paths) {
                for ((method, operation) in path.value.readOperationsMap()) {
                    val security = operation.security ?: swagger.openAPI.security
                    row {
                        cell(richText(method.name, code = true, color = Green))
                        cell(richText(path.key, code = true, color = Default))
                        cell(security?.flatMap { it.keys }?.joinToString(", ") ?: "")
                        cell(operation.summary ?: operation.description ?: "")
                    }
                }
            }
        }
    }

    private fun BlocksBuilder.operationSection(path: String, method: String, operation: Operation) {
        paragraph("") {
            heading1(operation.summary ?: "[please add summary]")

            paragraph(
                richText(" $method ", code = true, bold = true, color = Green),
                richText(" $path", code = true, color = Default)
            )

            paragraph(operation.description ?: "")
        }

        paragraph("") {
            operationAuth(operation)
            operationParams(operation)
            operationRequestSection(operation)
        }

        paragraph("") {
            operationResponseSection(operation)
        }
    }

    private fun BlocksBuilder.operationRequestSection(operation: Operation) {
        operation.requestBody?.let { request ->
            heading3("Request")
            request.description?.let { desc ->
                paragraph(desc)
            }
            request.content?.let { contents ->
                for ((contentType, content) in contents) {
                    paragraph(richText("Content-Type: "), richText(contentType, code = true, color = Default))

                    content.schema.externalDocs?.let {
                        paragraph(richText("Documentation: ", bold = true), richText(it.description ?: it.url, link = it.url))
                    }

                    propertiesRow("", content.schema)
                    divider()
                    exampleItem(content)
                }
            }
        }
    }

    private fun BlocksBuilder.operationResponseSection(operation: Operation) {
        operation.responses?.let { responses ->
            heading3("Response")

            for ((code, response) in responses) {
                operationResponseBody(response, code)
            }
        }
    }

    private fun BlocksBuilder.operationResponseBody(response: ApiResponse, code: String) {
        response.content?.takeIf { it.isNotEmpty() }?.let { contents ->
            for ((contentType, content) in contents) {
                responseBodyHeader(code, response, contentType)
                propertiesRow("", content.schema)
                divider()
                exampleItem(content)
            }
        } ?: run {
            responseBodyHeader(code, response, null)
        }
        paragraph(" ")
    }

    private fun BlocksBuilder.responseBodyHeader(code: String, response: ApiResponse, contentType: String?) {
        quote(
            richText(
                "$code ${response.description ?: ""}",
                code = true,
                bold = true,
                color = if (code.startsWith("2")) Green else Orange
            ),
            richText(contentType?.let { "  Content-Type: " } ?: "  No Content", color = Default),
            richText(contentType ?: "", code = contentType != null, color = Default),
            color = if (code.startsWith("2")) BlockColor.Green else BlockColor.Orange
        )
    }

    private fun BlocksBuilder.operationParams(operation: Operation) {
        if (!operation.parameters.isNullOrEmpty()) {
            heading3("Parameters")
            quote("Bold parameters are required", color = BlockColor.Gray)
            table(4, hasRowHeader = true, hasColumnHeader = true) {
                row {
                    cell(richText("Name"))
                    cell(richText("Type"))
                    cell(richText("Location"))
                    cell(richText("Description"))
                }
                for (parameter in operation.parameters) {
                    val required = parameter.required == true
                    row {
                        cell(richText(parameter.name, bold = required, code = true, color = Default))
                        cell(richText(parameter.schema?.type ?: ""))
                        cell(richText(parameter.`in`))
                        cell(richText(parameter.description ?: ""))
                    }
                }
            }
        }
    }

    private fun BlocksBuilder.operationAuth(operation: Operation) {
        val security = operation.security ?: swagger.openAPI.security
        security?.let { _ ->
            heading3("Authentication")
            security.flatMap { it.keys }.forEach {
                bullet(it)
            }
        }
    }

    private fun BlocksBuilder.componentsSection(schemas: Map<String, Schema<*>>) {
        heading1("Schemas")

        for ((name, schema) in schemas) {
            heading3(name)
            schema.description?.let { desc ->
                paragraph(desc)
            }
            propertiesRow("", schema)
        }

    }

    private fun BlocksBuilder.authenticationSection() {
        heading1("Authentication")

        for ((name, security) in swagger.openAPI.components.securitySchemes) {
            heading2(name)

            security.description?.let { desc ->
                paragraph(desc)
            }

            table(2, hasRowHeader = true) {
                row {
                    cell(richText("Type"))
                    cell(richText(security.type.toString()))
                }
                security.`in`?.let {
                    row {
                        cell(richText("In"))
                        cell(richText(it.toString()))
                    }
                    row {
                        cell(richText("Name"))
                        cell(richText(security.name ?: ""))
                    }
                }
                security?.openIdConnectUrl?.let { url ->
                    row {
                        cell(richText("Connect URL"))
                        cell(richText(url))
                    }
                }
                security.flows?.let { flows ->
                    // TODO oauth2 flows
                }
                security.scheme?.let { scheme ->
                    row {
                        cell(richText("Scheme"))
                        cell(richText(scheme))
                    }
                }
            }
        }
    }


    private fun BlocksBuilder.propertiesRow(path: String, schema: Schema<*>) {
        schema.`$ref`?.let { ref ->
            val resolvedSchema = swagger.resolveSchema(ref)
            consumedComponents.add(resolvedSchema)
            propertiesRow(path, resolvedSchema)
            return
        }

        when (schema) {
            is ObjectSchema -> {
                schema.properties?.forEach { (property, value) ->
                    propertiesRowItem(path, property, value, schema)
                }
            }

            is MapSchema, is JsonSchema -> {
                schema.properties?.forEach { (property, value) ->
                    propertiesRowItem(path, property, value, schema)
                }
                schema.additionalProperties?.let { additionalProperties ->
                    if (additionalProperties !is Schema<*>) return@let
                    propertiesRowItem(path, "<*>", additionalProperties)
                }
            }

            else -> {
                propertiesRowItem(path, "", schema)
            }
        }
    }

    private fun BlocksBuilder.propertiesRowItem(path: String, property: String, value: Schema<*>, parentSchema: Schema<*>? = null) {
        val rowPath = "$path.$property".removePrefix(".").removeSuffix(".")
        val required = parentSchema?.required?.contains(property) == true
        val example = value.example?.toString()?.takeIf { it.isNotBlank() }
        val description = value.description ?: ""
        val oneliner = example == null || description.length + example.length < 70 || description.isBlank()
        val defaultStr = value.default?.toString()?.takeIf { it.isNotBlank() }?.let { " (default: $it)" } ?: ""
        val component = value.`$ref`?.substringAfterLast("/")
            ?: value.items?.`$ref`?.substringAfterLast("/")?.let { "array<$it>" }

        divider()


        paragraph(
            richText(rowPath, code = true, color = Default),
            richText("  "),
            richText(component ?: value.type ?: value.types?.firstOrNull() ?: "", code = true, color = Pink),
            richText("  "),
            richText(if (required) "Required" else "Optional$defaultStr", code = true, color = if (required) Red else Green),
        )

        if (example != null) {
            if (oneliner) {
                paragraph(
                    richText("$description. "),
                    richText(" Example: ", bold = true),
                    richText(example, code = true, color = Blue)
                )
            } else {
                paragraph(richText(description))
                paragraph(richText("Example: ", bold = true), richText(example, code = true, color = Blue))
            }
        } else if (description.isNotBlank()) {
            paragraph(richText(description))
        }

        value.externalDocs?.let {
            paragraph(richText("Documentation: ", bold = true), richText(it.description ?: it.url, link = it.url))
        }

        if (!flatten && component != null) {
            return
        }

        if (value is ObjectSchema || value is MapSchema || value is JsonSchema) {
            propertiesRow(rowPath, value)
        }
        if (value is ArraySchema || value.items != null) {
            propertiesRow("$rowPath[]", value.items)
        }
    }

}