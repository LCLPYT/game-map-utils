package work.lclpnet.map_api.data.type

import com.mojang.serialization.Codec
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.DyeColor
import net.minecraft.util.math.Vec3d
import work.lclpnet.gaco.math.BlockFace
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.util.getRandomHsvColor
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_api.visual.createDataLabelDisplay
import java.util.*

object BlockFaceData : Data<BlockFace> {

    override fun id() = "block_face"

    override fun codec(): Codec<BlockFace> = BlockFace.CODEC

    override fun type() = BlockFace::class.java

    override fun display(
        value: BlockFace,
        visualizer: Visualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable {
        val color = if (propertyId != null) {
            getRandomHsvColor(Random(propertyId.hashCode().toLong()))
        } else DyeColor.YELLOW.entityColor

        val marker = visualizer.markBlockFace(value.pos, value.face, color)

        val textRef = if (propertyId != null) {
            val pos = Vec3d(value.pos.x + 0.5, value.pos.y + 0.5, value.pos.z + 0.5)
                .add(value.face.doubleVector)

            createDataLabelDisplay(visualizer, player, translations, propertyId, id, pos)
        } else null

        return Removable {
            visualizer.removeEntity(marker)

            if (textRef != null) {
                visualizer.removeEntity(textRef)
            }
        }
    }
}