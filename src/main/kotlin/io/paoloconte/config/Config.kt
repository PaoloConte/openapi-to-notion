package io.paoloconte.config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val generateCollection: String? = null,
    val pages: List<PageConfig>
)