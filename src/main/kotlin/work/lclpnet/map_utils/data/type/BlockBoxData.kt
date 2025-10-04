package work.lclpnet.map_utils.data.type

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
import work.lclpnet.map_utils.data.Data
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.editor.type.BlockBoxEditor
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.visual.Visualizer
import work.lclpnet.map_utils.visual.createDataLabelDisplay

object BlockBoxData : Data<BlockBox> {

    override fun id() = "block_box"

    override fun codec(): Codec<BlockBox> = BlockBox.CODEC

    override fun createEditor(args: SessionArgs, visualizer: Visualizer) =
        BlockBoxEditor(args, visualizer)

    override fun display(
        value: BlockBox,
        visualizer: Visualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable {
        val marker = DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, visualizer.world())

        marker.blockState = Blocks.GREEN_STAINED_GLASS.defaultState
        marker.setBrightness(Brightness(15, 15))
        marker.setPos(value.min().x.toDouble(), value.min().y.toDouble(), value.min().z.toDouble())
        marker.setTransformation(
            AffineTransformation(
                Matrix4f()
                    .scale(value.width().toFloat(), value.height().toFloat(), value.length().toFloat())
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
