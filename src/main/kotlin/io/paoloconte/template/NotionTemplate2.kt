package io.paoloconte.template

import io.paoloconte.notion.BlocksBuilder
import io.paoloconte.notion.BlocksBuilder.RowsBuilder
import io.paoloconte.notion.blocks
import io.paoloconte.notion.richText
import io.paoloconte.openapi.resolveSchema
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.parser.core.models.SwaggerParseResult
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.common.BlockColor
import notion.api.v1.model.common.RichTextColor.*

class NotionTemplate2(
    private val swagger: SwaggerParseResult,
    private val fileName: String,
    private val flatten: Boolean
) : Template {

    private val consumedComponents = mutableListOf<Schema<*>>()

    override fun render(): List<Block> = blocks {

        pageHeader(fileName)
        summarySection()

        for (path in swagger.openAPI.paths) {
            for ((method, operation) in path.value.readOperationsMap()) {
                operationSection(path.key, method.name, operation)
            }
        }

        val components = swagger.openAPI.components.schemas
            ?.filter { (_, schema) -> !consumedComponents.contains(schema) }
            ?: emptyMap()
        if (!flatten && components.isNotEmpty()) {
            componentsSection(components)
        }

        authenticationSection()
    }

    private fun BlocksBuilder.pageHeader(fileName: String) {
        callout(
            richText("This page is automatically generated from the OpenAPI specification.\n"),
            richText("Do not edit!\n"),
            richText("File: "), richText(fileName, code = true, color = Default),
            icon = "\u2728"
        )
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
        heading1(operation.summary ?: "[please add summary]")

        paragraph(
            richText(" $method ", code = true, bold = true, color = Green),
            richText(" $path", code = true, color = Default)
        )

        paragraph(operation.description ?: "")

        operationAuth(operation)
        operationParams(operation)
        operationRequestSection(operation)

        operationResponseSection(operation)
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

                    schemaTable(content.schema)

                    exampleItem(content.example)
                }
            }
        }
    }

    private fun BlocksBuilder.schemaTable(schema: Schema<*>) {
        table(3, hasColumnHeader = true) {
            row {
                cell(richText("Name"))
                cell(richText("Type"))
                cell(richText("Description"))
            }
            propertiesRow("", schema)
        }
    }

    private fun BlocksBuilder.operationResponseSection(operation: Operation) {
        operation.responses?.let { responses ->
            heading3("Response")

            for ((code, response) in responses) {
                operationResponseBody(response, code)
                divider()
            }
        }
    }

    private fun BlocksBuilder.operationResponseBody(response: ApiResponse, code: String) {
        response.content?.takeIf { it.isNotEmpty() }?.let { contents ->
            for ((contentType, content) in contents) {
                responseBodyHeader(code, response, contentType)
                schemaTable(content.schema)
                exampleItem(content.example)
            }
        } ?: run {
            responseBodyHeader(code, response, null)
        }
    }

    private fun BlocksBuilder.exampleItem(example: Any?) {
        example ?: return
        toggle("Example") {
            codeBlock(language = "json", content = example.toString().trim())
        }
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
                        cell(richText(parameter.name + if(required) "" else "?", code = true, color = Default))
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
            schemaTable(schema)
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


    private fun RowsBuilder.propertiesRow(path: String, schema: Schema<*>) {
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

    private fun RowsBuilder.propertiesRowItem(path: String, property: String, value: Schema<*>, parentSchema: Schema<*>? = null) {
        val rowPath = "$path.$property".removePrefix(".").removeSuffix(".")
        val required = parentSchema?.required?.contains(property) == true
        val description = value.description ?: ""
        val defaultStr = value.default?.toString()?.takeIf { it.isNotBlank() }?.let { " (default: $it)" } ?: ""
        val component = value.`$ref`?.substringAfterLast("/")
            ?: value.items?.`$ref`?.substringAfterLast("/")?.let { "array<$it>" }
        val type = component ?: value.type ?: value.types?.firstOrNull() ?: ""
        val requiredStr = if (required) "" else "?"


        row {
            cell(
                richText(rowPath.dropLastWhile { it != '.' }, code = true, color = Default),
                richText(rowPath.takeLastWhile { it != '.' }, code = true, color = Default, bold = true)
            )
            cell(richText(type+requiredStr, code = true, color = Pink))
            cell(
                richText("$description$defaultStr "),
                value.externalDocs?.let { richText(it.description ?: "Docs", link = it.url) }
            )
        }

        /*
        */

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