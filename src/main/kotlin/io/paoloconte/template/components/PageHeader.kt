package io.paoloconte.template.components

import io.paoloconte.notion.BlocksBuilder
import io.paoloconte.notion.richText
import notion.api.v1.model.common.RichTextColor.Default


internal fun BlocksBuilder.pageHeader(fileName: String, visible: Boolean) {
    if (!visible) return
    callout(
        richText("This page is automatically generated from the OpenAPI specification.\n"),
        richText("Do not edit!\n"),
        richText("File: "), richText(fileName, code = true, color = Default),
        icon = "\u2728"
    )
}