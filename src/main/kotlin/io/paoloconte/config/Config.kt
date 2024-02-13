package io.paoloconte.config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val generateCollection: String? = null,
    val template: Int = 2,
    val pages: List<PageConfig>
)