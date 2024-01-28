package io.paoloconte

import io.paoloconte.BlocksBuilder.RowsBuilder
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.core.models.SwaggerParseResult
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.common.BlockColor
import notion.api.v1.model.common.RichTextColor.*

object NotionTemplate {

    fun render(swagger: SwaggerParseResult, fileName: String): List<Block> {
        return blocks {

            callout(
                richText("This page is automatically generated from the OpenAPI specification. Do not edit!\n"),
                richText("File: "), richText(fileName, code = true, color = Default),
                icon = "\uD83D\uDCA1"
            )

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

            heading1("Endpoints")

            for (path in swagger.openAPI.paths) {
                for ((method, operation) in path.value.readOperationsMap()) {
                    heading2(operation.summary ?: "[please add summary]")

                    paragraph(
                        richText(" ${method.name} ", code = true, bold = true, color = Green),
                        richText(" ${path.key}", code = true, color = Default)
                    )

                    paragraph(operation.description ?: "")

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

                                quote("Bold fields are required", color = BlockColor.Gray)
                                table(4, hasColumnHeader = true, hasRowHeader = true) {
                                    row {
                                        cell(richText("Name"))
                                        cell(richText("Type"))
                                        cell(richText("Description"))
                                        cell(richText("Example"))
                                    }
                                    objectRows("", content.schema)

                                }
                            }
                        }
                    }

                    operation.responses?.let { responses ->
                        heading3("Response")

                        for ((code, response) in responses) {
                            paragraph(
                                richText(
                                    "$code ${response.description ?: ""}",
                                    code = true,
                                    bold = true,
                                    color = if (code.startsWith("2")) Green else Orange
                                )
                            )
                            response.content?.let { contents ->
                                for ((contentType, content) in contents) {
                                    paragraph(richText("Content-Type: "), richText(contentType, code = true, color = Default))

                                    quote("Bold fields are required", color = BlockColor.Gray)
                                    table(4, hasColumnHeader = true, hasRowHeader = true) {
                                        row {
                                            cell(richText("Name"))
                                            cell(richText("Type"))
                                            cell(richText("Description"))
                                            cell(richText("Example"))
                                        }
                                        objectRows("", content.schema)

                                    }
                                }
                            }
                        }
                    }
                }
            }

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

    private fun RowsBuilder.objectRows(path: String, schema: Schema<*>) {
        if (schema is ObjectSchema) {
            schema.properties?.forEach { (property, value) ->
                val rowPath = "$path.$property".removePrefix(".")
                val required = schema.required?.contains(property) == true
                row {
                    cell(richText(rowPath, bold = required, code = true, color = Default))
                    cell(value.type)
                    cell(value.description ?: "")
                    cell(value.example?.toString() ?: "")
                }
                if (value is ObjectSchema) {
                    objectRows(rowPath, value)
                }
                if (value is ArraySchema) {
                    objectRows("$rowPath[]", value.items)
                }
            }
        } else {
            row {
                cell(richText(path, bold = true))
                cell(schema.type)
                cell(schema.description ?: "")
                cell(schema.example?.toString() ?: "")
            }
        }
    }


}