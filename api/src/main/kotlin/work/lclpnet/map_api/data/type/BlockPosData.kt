package work.lclpnet.map_api.data.type

import com.mojang.serialization.Codec
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
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
        player: ServerPlayerEntity,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable {
        val color = getRandomHsvColor(if (propertyId != null) Random(propertyId.hashCode().toLong()) else Random())

        val state = visualizer.world().getBlockState(value)
        val marker = visualizer.markBlock(value, state, color)

        val textRef = if (propertyId != null)
            createDataLabelDisplay(visualizer, player, translations, propertyId, id, Vec3d(value.x + 0.5, value.y + 1.35, value.z + 0.5))
        else null

        return Removable {
            visualizer.removeEntity(marker)

            if (textRef != null) {
                visualizer.removeEntity(textRef)
            }
        }
    }
}