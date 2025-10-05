package work.lclpnet.map_api.data.type

import com.mojang.serialization.Codec
import net.minecraft.server.network.ServerPlayerEntity
import work.lclpnet.gaco.ds.Checkpoint
import work.lclpnet.kibu.hook.util.PositionRotation
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer

object CheckpointData : Data<Checkpoint> {

    override fun id(): String = "checkpoint"

    override fun codec(): Codec<Checkpoint> = Checkpoint.CODEC

    override fun display(
        value: Checkpoint,
        visualizer: Visualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable {
        val posRot = PositionRotation(value.pos.x, value.pos.y, value.pos.z, value.yaw, value.pitch)

        val respawnPosRemovable = PositionData.display(posRot, visualizer, player, translations, id, propertyId)
        val boundsRemovable = BlockBoxData.display(value.bounds, visualizer, player, translations, id, null)

        return Removable {
            respawnPosRemovable.remove()
            boundsRemovable.remove()
        }
    }
}