package io.paoloconte.app

import io.paoloconte.notion.BlocksBuilder
import io.paoloconte.notion.blocks
import io.paoloconte.notion.richText
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.core.models.SwaggerParseResult
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.common.BlockColor
import notion.api.v1.model.common.RichTextColor.*

object NotionTemplate {

    fun render(swagger: SwaggerParseResult, fileName: String): List<Block> = blocks {
        // wrap everything inside a paragraph, so it can be easily deleted with few calls

        paragraph("") {
            callout(
                richText("This page is automatically generated from the OpenAPI specification.\n"),
                richText("Do not edit!\n"),
                richText("File: "), richText(fileName, code = true, color = Default),
                icon = "\u2728"
            )
        }

        paragraph("") {
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

        for (path in swagger.openAPI.paths) {
            for ((method, operation) in path.value.readOperationsMap()) {
                paragraph("") {
                    heading2(operation.summary ?: "[please add summary]")

                    paragraph(
                        richText(" ${method.name} ", code = true, bold = true, color = Green),
                        richText(" ${path.key}", code = true, color = Default)
                    )

                    paragraph(operation.description ?: "")

                }

                paragraph("") {
                    val security = operation.security ?: swagger.openAPI.security
                    security?.let { _ ->
                        heading3("Authentication")
                        security.flatMap { it.keys }.forEach {
                            bullet(it)
                        }
                    }

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

                    operation.requestBody?.let { request ->
                        heading3("Request")
                        request.description?.let { desc ->
                            paragraph(desc)
                        }
                        request.content?.let { contents ->
                            for ((contentType, content) in contents) {
                                paragraph(richText("Content-Type: "), richText(contentType, code = true, color = Default))

                                propertiesRow("", content.schema)
                                divider()

                                content.example?.let { example ->
                                    toggle("Example") {
                                        codeBlock(language = "json", content = example.toString().trim())
                                    }
                                }
                            }
                        }
                    }
                }

                paragraph("") {
                    operation.responses?.let { responses ->
                        heading3("Response")

                        for ((code, response) in responses) {
                            response.content?.takeIf { it.isNotEmpty() }?.let { contents ->
                                for ((contentType, content) in contents) {
                                    quote(
                                        richText(
                                            "$code ${response.description ?: ""}",
                                            code = true,
                                            bold = true,
                                            color = if (code.startsWith("2")) Green else Orange
                                        ),
                                        richText("  Content-Type: ", color = Default),
                                        richText(contentType, code = true, color = Default),
                                        color = if (code.startsWith("2")) BlockColor.Green else BlockColor.Orange
                                    )

                                    propertiesRow("", content.schema)
                                    divider()

                                    content.example?.let { example ->
                                        toggle("Example") {
                                            codeBlock(language = "json", content = example.toString().trim())
                                        }
                                    }
                                }
                            } ?: run {
                                quote(
                                    richText(
                                        "$code ${response.description ?: ""}",
                                        code = true,
                                        bold = true,
                                        color = if (code.startsWith("2")) Green else Orange
                                    ),
                                    richText("  No Content ", color = Default, italic = true),
                                    color = if (code.startsWith("2")) BlockColor.Green else BlockColor.Orange
                                )
                            }
                            paragraph(" ")
                        }
                    }
                }
            }
        }

        paragraph("") {
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

    }


    private fun BlocksBuilder.propertiesRow(path: String, schema: Schema<*>) {
        if (schema is ObjectSchema) {
            schema.properties?.forEach { (property, value) ->
                propertiesRowItem(path, property, value, schema)
            }
        } else {
            propertiesRowItem(path, "", schema)
        }
    }

    private fun BlocksBuilder.propertiesRowItem(path: String, property: String, value: Schema<*>, parentSchema: Schema<*>? = null) {
        val rowPath = "$path.$property".removePrefix(".").removeSuffix(".")
        val required = parentSchema?.required?.contains(property) == true
        val example = value.example?.toString()?.takeIf { it.isNotBlank() }
        val description = value.description ?: ""
        val oneliner = example == null || description.length + example.length < 80 || description.isBlank()

        divider()

        paragraph(
            richText(rowPath, code = true, color = Default),
            richText("  "),
            richText(value.type, code = true, color = Pink),
            richText("  "),
            richText(if (required) "Required" else "Optional", code = true, color = if (required) Red else Green),
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


        if (value is ObjectSchema) {
            propertiesRow(rowPath, value)
        }
        if (value is ArraySchema) {
            propertiesRow("$rowPath[]", value.items)
        }
    }


}