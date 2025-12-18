package work.lclpnet.map_api.data.type

import com.mojang.math.Transformation
import com.mojang.serialization.Codec
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Blocks
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

    override fun type() = BlockBox::class.java

    override fun display(
        value: BlockBox,
        visualizer: Visualizer,
        player: ServerPlayer,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable {
        val marker = Display.BlockDisplay(EntityType.BLOCK_DISPLAY, visualizer.world())

        val margin = -0.015f

        marker.blockState = Blocks.GREEN_STAINED_GLASS.defaultBlockState()
        marker.brightnessOverride = Brightness(15, 15)
        marker.setPosRaw(
            value.min().x.toDouble() + margin,
            value.min().y.toDouble() + margin,
            value.min().z.toDouble() + margin
        )
        marker.setTransformation(
            Transformation(
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
