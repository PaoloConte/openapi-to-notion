package io.paoloconte.template

import notion.api.v1.model.blocks.Block

interface Template {
    fun render(): List<Block>
}