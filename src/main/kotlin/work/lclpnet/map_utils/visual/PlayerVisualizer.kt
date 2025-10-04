package work.lclpnet.map_utils.visual

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.AffineTransformation
import net.minecraft.util.math.Vec3i
import org.joml.Matrix4f
import work.lclpnet.gaco.dynamic_entities.DynamicEntity
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.gaco.dynamic_entities.PlayerSpecificDynamicEntity
import work.lclpnet.map_utils.editor.SessionArgs

class PlayerVisualizer(
    val args: SessionArgs,
    val dynamicEntityManager: DynamicEntityManager,
    val sceneRenderer: PlayerSceneRenderer
) : Visualizer, SceneRenderer by sceneRenderer {

    val entities = mutableSetOf<DynamicEntity>()
    val mapping = mutableMapOf<Entity, DynamicEntity>()

    override fun world(): ServerWorld = args.world

    override fun addEntity(entity: Entity) {
        val dynamicEntity = PlayerSpecificDynamicEntity(entity, args.player().uuid)

        entities.add(dynamicEntity)
        mapping[entity] = dynamicEntity

        dynamicEntityManager.add(dynamicEntity)
    }

    override fun removeEntity(entity: Entity) {
        val dynamicEntity = mapping.remove(entity) ?:  return

        entities.remove(dynamicEntity)
        dynamicEntityManager.remove(dynamicEntity)
    }

    override fun markBlock(pos: Vec3i, state: BlockState, glowColor: Int): DisplayEntity.BlockDisplayEntity {
        val state = if (state.isAir || state.isOf(Blocks.BARRIER) || state.isOf(Blocks.STRUCTURE_VOID)) {
            Blocks.GLASS.defaultState
        } else state

        val margin = 0.015f
        val marker = DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, args.world)
        marker.setPosition(
            pos.x.toDouble() + margin,
            pos.y.toDouble() + margin,
            pos.z.toDouble() + margin
        )
        marker.setTransformation(AffineTransformation(Matrix4f().scale(1f - 2 * margin)))
        marker.blockState = state
        marker.isGlowing = true
        marker.glowColorOverride = glowColor

        addEntity(marker)

        return marker
    }

    override fun destroy() {
        for (entity in entities) {
            dynamicEntityManager.remove(entity)
        }

        sceneRenderer.destroy()
    }
}