package work.lclpnet.map_utils.data.type

import com.mojang.serialization.Codec
import work.lclpnet.map_utils.data.Data
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.editor.Visualizer
import work.lclpnet.map_utils.editor.type.PositionedBlockSetEditor
import work.lclpnet.map_utils.util.PositionedBlockSet

class PositionedBlockSetData : Data<PositionedBlockSet> {

    override fun id() = "positioned_block_set"

    override fun codec(): Codec<PositionedBlockSet> = PositionedBlockSet.CODEC

    override fun createEditor(args: SessionArgs, visualizer: Visualizer, propertyId: String?) =
        PositionedBlockSetEditor(this, args, visualizer, propertyId, propertyId)
}