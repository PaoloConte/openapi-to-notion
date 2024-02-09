package io.paoloconte.config

import kotlinx.serialization.Serializable

@Serializable
data class PageConfig (
    val notionPageId: String,
    val apiFolder: String,
)