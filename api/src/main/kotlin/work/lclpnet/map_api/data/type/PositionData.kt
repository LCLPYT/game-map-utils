package work.lclpnet.map_api.data.type

import com.mojang.math.Transformation
import com.mojang.serialization.Codec
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ObserverBlock
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import work.lclpnet.kibu.hook.util.PositionRotation
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.util.getRandomHsvColor
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_api.visual.createDataLabelDisplay
import java.util.*

object PositionData : Data<PositionRotation> {

    override fun id() = "position"

    override fun codec(): Codec<PositionRotation> = PositionRotation.CODEC

    override fun type() = PositionRotation::class.java

    override fun display(
        value: PositionRotation,
        visualizer: Visualizer,
        player: ServerPlayer,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable {
        val random = if (propertyId != null) Random(propertyId.hashCode().toLong()) else Random()

        val marker = Display.BlockDisplay(EntityType.BLOCK_DISPLAY, visualizer.world())
        marker.setPos(value.x(), value.y() + 0.125, value.z())
        marker.setTransformation(
            Transformation(Matrix4f()
            .scale(.25f)
            .rotate(Math.toRadians(-value.yaw.toDouble()).toFloat(), 0f, 1f, 0f)
            .rotate(Math.toRadians(value.pitch.toDouble()).toFloat(), 1f, 0f, 0f)
            .translate(-0.5f, -0.5f, -0.5f)
        ))

        marker.blockState = Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, Direction.SOUTH)
        marker.setGlowingTag(true)
        marker.glowColorOverride = getRandomHsvColor(random)

        visualizer.addEntity(marker)

        val textRef = if (propertyId != null)
            createDataLabelDisplay(visualizer, player, translations, propertyId, id, Vec3(value.x(), value.y() + 0.35, value.z()))
        else null

        return Removable {
            visualizer.removeEntity(marker)

            if (textRef != null) {
                visualizer.removeEntity(textRef)
            }
        }
    }
}