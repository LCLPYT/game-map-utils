package work.lclpnet.map_api.data.type

import com.mojang.serialization.Codec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.DyeColor
import net.minecraft.world.phys.Vec3
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.util.getRandomHsvColor
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_api.visual.createDataLabelDisplay
import java.util.*

object BlockPosData : Data<BlockPos> {

    override fun id() = "block_pos"

    override fun codec(): Codec<BlockPos> = BlockPos.CODEC

    override fun type() = BlockPos::class.java

    override fun display(
        value: BlockPos,
        visualizer: Visualizer,
        player: ServerPlayer,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable {
        val color = if (propertyId != null) {
            getRandomHsvColor(Random(propertyId.hashCode().toLong()))
        } else DyeColor.LIME.textureDiffuseColor

        val marker = visualizer.markBlock(value, color)

        val textRef = if (propertyId != null)
            createDataLabelDisplay(visualizer, player, translations, propertyId, id, Vec3(value.x + 0.5, value.y + 1.35, value.z + 0.5))
        else null

        return Removable {
            visualizer.removeEntity(marker)

            if (textRef != null) {
                visualizer.removeEntity(textRef)
            }
        }
    }
}