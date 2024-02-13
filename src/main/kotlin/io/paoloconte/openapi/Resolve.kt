package io.paoloconte.openapi

import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.core.models.SwaggerParseResult


fun SwaggerParseResult.resolveSchema(ref: String): Schema<*> {
    val components = openAPI.components
    val schema = components.schemas[ref]
    if (schema != null) return schema
    val refSchema = components.schemas[ref.substringAfterLast("/")]
    if (refSchema != null) return refSchema
    error("Could not resolve schema $ref")
}