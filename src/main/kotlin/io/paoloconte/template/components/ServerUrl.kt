package io.paoloconte.template.components

import io.paoloconte.notion.BlocksBuilder
import io.paoloconte.notion.richText
import io.swagger.v3.oas.models.servers.Server
import notion.api.v1.model.common.RichTextColor.Blue

internal fun BlocksBuilder.serverUrl(servers: MutableList<Server>?) {
    val servers = servers?.filter { it.url != "/" }
    if (servers.isNullOrEmpty()) return
    heading3("Servers")
    servers.forEach { server ->
        paragraph(richText(if (server.description.isNullOrBlank()) "" else "${server.description}: "), richText(server.url, code = true, color = Blue))
    }
}