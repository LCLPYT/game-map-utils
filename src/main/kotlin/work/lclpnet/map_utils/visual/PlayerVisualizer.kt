package work.lclpnet.map_utils.visual

import com.mojang.math.Transformation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.joml.Matrix4f
import org.joml.Quaternionf
import work.lclpnet.gaco.dynamic_entities.DynamicEntity
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.gaco.dynamic_entities.PlayerSpecificDynamicEntity
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.level.BlockModificationHooks
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
    val markedBlocks = mutableMapOf<Vec3i, Display.BlockDisplay>()
    val markedBlockFaces = mutableMapOf<Vec3i, MutableMap<Direction, Display.BlockDisplay>>()

    override fun world(): ServerLevel = args.world
    
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

        val blockPos = entity.blockPosition()
        val blockMarker = markedBlocks[blockPos]
        
        if (blockMarker == entity) {
            markedBlocks.remove(blockPos)
        }
    }

    override fun markBlock(pos: Vec3i, state: BlockState, glowColor: Int): Display.BlockDisplay {
        val prev = markedBlocks[pos]
        
        if (prev != null) {
            removeEntity(prev)
        }

        val margin = 0.015f
        val marker = Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, args.world)
        marker.setPos(
            pos.x.toDouble() + margin,
            pos.y.toDouble() + margin,
            pos.z.toDouble() + margin
        )
        marker.setTransformation(Transformation(Matrix4f().scale(1f - 2 * margin)))
        marker.blockState = getMarkerState(state)
        marker.setGlowingTag(true)
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
    ): Display.BlockDisplay {
        val markedFaces = markedBlockFaces.computeIfAbsent(pos) { mutableMapOf() }
        val prev = markedFaces[face]

        if (prev != null) {
            removeEntity(prev)
        }

        val margin = 0.015f
        val marker = Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, args.world)

        marker.setPos(
            pos.x.toDouble() + 0.5,
            pos.y.toDouble() + 0.5,
            pos.z.toDouble() + 0.5
        )

        marker.setTransformation(
            Transformation(Matrix4f()
            .rotate(Quaternionf().rotationTo(
                Direction.NORTH.unitVec3f,
                face.unitVec3f
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
        marker.setGlowingTag(true)
        marker.glowColorOverride = glowColor

        addEntity(marker)

        markedFaces[face] = marker

        return marker
    }

    fun getMarkerState(state: BlockState): BlockState = when {
        state.isAir || state.`is`(Blocks.BARRIER) || state.`is`(Blocks.STRUCTURE_VOID) -> {
            Blocks.GLASS.defaultBlockState()
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