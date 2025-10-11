package work.lclpnet.map_api.data.type

import com.mojang.serialization.Codec
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.Brightness
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.AffineTransformation
import org.joml.Matrix4f
import work.lclpnet.gaco.ds.BlockBox
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_api.visual.createDataLabelDisplay

object BlockBoxData : Data<BlockBox> {

    override fun id() = "block_box"

    override fun codec(): Codec<BlockBox> = BlockBox.CODEC

    override fun display(
        value: BlockBox,
        visualizer: Visualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable {
        val marker = DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, visualizer.world())

        val margin = -0.015f

        marker.blockState = Blocks.GREEN_STAINED_GLASS.defaultState
        marker.setBrightness(Brightness(15, 15))
        marker.setPos(
            value.min().x.toDouble() + margin,
            value.min().y.toDouble() + margin,
            value.min().z.toDouble() + margin
        )
        marker.setTransformation(
            AffineTransformation(
                Matrix4f().scale(
                    value.width().toFloat() - 2 * margin,
                    value.height().toFloat() - 2 * margin,
                    value.length().toFloat() - 2 * margin
                )
            )
        )

        visualizer.addEntity(marker)

        val textRef = if (propertyId != null)
            createDataLabelDisplay(visualizer, player, translations, propertyId, id, value.toBox().center)
        else null

        return Removable {
            visualizer.removeEntity(marker)

            if (textRef != null) {
                visualizer.removeEntity(textRef)
            }
        }
    }
}
