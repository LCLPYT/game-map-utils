package work.lclpnet.map_utils.data.type

import com.mojang.serialization.Codec
import work.lclpnet.gaco.ds.BlockBox
import work.lclpnet.map_utils.data.Data
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.editor.Visualizer
import work.lclpnet.map_utils.editor.type.BlockBoxEditor

object BlockBoxData : Data<BlockBox> {

    override fun id() = "block_box"

    override fun codec(): Codec<BlockBox> = BlockBox.CODEC

    override fun createEditor(args: SessionArgs, visualizer: Visualizer) =
        BlockBoxEditor(this, args, visualizer)
}
