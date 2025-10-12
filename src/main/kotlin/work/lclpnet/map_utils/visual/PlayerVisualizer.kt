package work.lclpnet.map_utils.visual

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.AffineTransformation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i
import org.joml.Matrix4f
import org.joml.Quaternionf
import work.lclpnet.gaco.dynamic_entities.DynamicEntity
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.gaco.dynamic_entities.PlayerSpecificDynamicEntity
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.world.BlockModificationHooks
import work.lclpnet.map_api.visual.SceneRenderer
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_utils.editor.SessionArgs

class PlayerVisualizer(
    val args: SessionArgs,
    val dynamicEntityManager: DynamicEntityManager,
    val sceneRenderer: PlayerSceneRenderer
) : Visualizer, SceneRenderer by sceneRenderer {

    val entities = mutableSetOf<DynamicEntity>()
    val mapping = mutableMapOf<Entity, DynamicEntity>()
    val markedBlocks = mutableMapOf<Vec3i, DisplayEntity.BlockDisplayEntity>()
    val markedBlockFaces = mutableMapOf<Vec3i, MutableMap<Direction, DisplayEntity.BlockDisplayEntity>>()

    override fun world(): ServerWorld = args.world
    
    fun init(hooks: HookRegistrar) {
        hooks.registerHook(
            BlockModificationHooks.BLOCK_BROKEN,
            BlockModificationHooks.BlockModifiedHook { world, pos, _ ->
                if (world == args.world) {
                    updateMarkedBlock(pos)
                }
            }
        )

        hooks.registerHook(
            BlockModificationHooks.BLOCK_PLACED,
            BlockModificationHooks.BlockModifiedHook { world, pos, _ ->
                if (world == args.world) {
                    updateMarkedBlock(pos)
                }
            }
        )
    }
    
    fun updateMarkedBlock(pos: BlockPos) {
        val state = getMarkerState(args.world.getBlockState(pos))

        val posMarker = markedBlocks[pos]

        if (posMarker != null) {
            posMarker.blockState = state
        }

        val sideMarkers = markedBlockFaces[pos]

        if (sideMarkers != null) {
            for (marker in sideMarkers.values) {
                marker.blockState = state
            }
        }
    }

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

        val blockPos = entity.blockPos
        val blockMarker = markedBlocks[blockPos]
        
        if (blockMarker == entity) {
            markedBlocks.remove(blockPos)
        }
    }

    override fun markBlock(pos: Vec3i, state: BlockState, glowColor: Int): DisplayEntity.BlockDisplayEntity {
        val prev = markedBlocks[pos]
        
        if (prev != null) {
            removeEntity(prev)
        }

        val margin = 0.015f
        val marker = DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, args.world)
        marker.setPosition(
            pos.x.toDouble() + margin,
            pos.y.toDouble() + margin,
            pos.z.toDouble() + margin
        )
        marker.setTransformation(AffineTransformation(Matrix4f().scale(1f - 2 * margin)))
        marker.blockState = getMarkerState(state)
        marker.isGlowing = true
        marker.glowColorOverride = glowColor

        addEntity(marker)
        
        markedBlocks[pos] = marker

        return marker
    }

    override fun markBlockFace(
        pos: Vec3i,
        face: Direction,
        state: BlockState,
        glowColor: Int
    ): DisplayEntity.BlockDisplayEntity {
        val markedFaces = markedBlockFaces.computeIfAbsent(pos) { mutableMapOf() }
        val prev = markedFaces[face]

        if (prev != null) {
            removeEntity(prev)
        }

        val margin = 0.015f
        val marker = DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, args.world)

        marker.setPosition(
            pos.x.toDouble() + 0.5,
            pos.y.toDouble() + 0.5,
            pos.z.toDouble() + 0.5
        )

        marker.setTransformation(AffineTransformation(Matrix4f()
            .rotate(Quaternionf().rotationTo(
                Direction.NORTH.floatVector,
                face.floatVector
            ))
            .translate(
                -0.5f + margin,
                -0.5f + margin,
                -0.5f + margin,
            )
            .scale(
                1f - 2 * margin,
                1f - 2 * margin,
                0f
            )
        ))

        marker.blockState = getMarkerState(state)
        marker.isGlowing = true
        marker.glowColorOverride = glowColor

        addEntity(marker)

        markedFaces[face] = marker

        return marker
    }

    fun getMarkerState(state: BlockState): BlockState = when {
        state.isAir || state.isOf(Blocks.BARRIER) || state.isOf(Blocks.STRUCTURE_VOID) -> {
            Blocks.GLASS.defaultState
        }
        else -> state
    }

    override fun destroy() {
        for (entity in entities) {
            dynamicEntityManager.remove(entity)
        }

        sceneRenderer.destroy()
    }
}