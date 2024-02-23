package io.paoloconte.template.components

import io.paoloconte.notion.BlocksBuilder
import io.paoloconte.notion.richText
import io.swagger.v3.oas.models.servers.Server
import notion.api.v1.model.common.RichTextColor.Blue

internal fun BlocksBuilder.serverUrl(servers: MutableList<Server>?) {
    servers?.firstOrNull()?.takeIf { it.url != "/" }?.let { server ->
        paragraph(richText("Server: "), richText(server.url, code = true, color = Blue))
    }
}