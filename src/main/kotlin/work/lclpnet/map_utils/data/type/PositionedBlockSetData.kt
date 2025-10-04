package work.lclpnet.map_utils.data.type

import com.mojang.serialization.Codec
import net.minecraft.entity.Entity
import net.minecraft.server.network.ServerPlayerEntity
import work.lclpnet.gaco.ds.PositionedBlockSet
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.Data
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.editor.type.PositionedBlockSetEditor
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.util.Visualizer
import work.lclpnet.map_utils.util.getRandomHsvColor
import java.util.*

object PositionedBlockSetData : Data<PositionedBlockSet> {

    override fun id() = "positioned_block_set"

    override fun codec(): Codec<PositionedBlockSet> = PositionedBlockSet.CODEC

    override fun createEditor(args: SessionArgs, visualizer: Visualizer) =
        PositionedBlockSetEditor(args, visualizer)

    override fun display(
        value: PositionedBlockSet,
        visualizer: Visualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        propertyId: String?
    ): Removable {
        val displays = mutableListOf<Entity>()
        val color = getRandomHsvColor(if (propertyId != null) Random(propertyId.hashCode().toLong()) else Random())

        for ((pos, state) in value) {
            val element = visualizer.markBlock(pos, state, color)
            displays.add(element)
        }

        return Removable {
            for (entity in displays) {
                visualizer.removeEntity(entity)
            }
        }
    }
}