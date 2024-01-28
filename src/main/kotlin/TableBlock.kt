package io.paoloconte

import com.google.gson.annotations.SerializedName
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.blocks.BlockParent
import notion.api.v1.model.blocks.BlockType
import notion.api.v1.model.blocks.TableRowBlock
import notion.api.v1.model.common.ObjectType
import notion.api.v1.model.users.User
import java.util.*

// adds the children property missing from library
// https://github.com/seratch/notion-sdk-jvm/issues/140
open class TableBlock(
    @SerializedName("object") override val objectType: ObjectType = ObjectType.Block,
    override val type: BlockType = BlockType.Table,
    override var id: String? = UUID.randomUUID().toString(),
    override var createdTime: String? = null,
    override var createdBy: User? = null,
    override var lastEditedTime: String? = null,
    override var lastEditedBy: User? = null,
    override var hasChildren: Boolean? = null,
    override var archived: Boolean? = null,
    override var parent: BlockParent? = null,
    val table: Element,
    override val requestId: String? = null,
) : Block {
    open class Element(
        var tableWidth: Int,
        var hasColumnHeader: Boolean,
        var hasRowHeader: Boolean,
        var children: List<TableRowBlock>
    )
}
