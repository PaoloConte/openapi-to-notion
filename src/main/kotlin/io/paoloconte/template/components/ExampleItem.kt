package io.paoloconte.template.components

import io.paoloconte.notion.BlocksBuilder
import io.paoloconte.notion.richText
import io.swagger.v3.oas.models.media.MediaType

internal fun BlocksBuilder.exampleItem(content: MediaType?) {
    val hasExamples = !content?.examples.isNullOrEmpty() || !content?.example?.toString().isNullOrEmpty()
    if (!hasExamples) return
    toggle("Examples") {
        content?.example?.let { example ->
            codeBlock(language = "json", content = example.toString().trim())
        }
        content?.examples?.values?.forEach { example ->
            if (!example.summary.isNullOrBlank())
                paragraph(richText(example.summary, bold = true))
            codeBlock(language = "json", content = example.value.toString().trim())
        }
    }
}
