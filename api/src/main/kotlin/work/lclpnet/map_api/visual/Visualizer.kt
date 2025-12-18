package work.lclpnet.map_api.visual

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState

interface Visualizer : SceneRenderer {

    fun world(): ServerLevel

    fun addEntity(entity: Entity)

    fun removeEntity(entity: Entity)

    fun markBlock(pos: Vec3i, state: BlockState, glowColor: Int): Display.BlockDisplay

    fun markBlockFace(pos: Vec3i, face: Direction, state: BlockState, glowColor: Int): Display.BlockDisplay

    fun markBlock(pos: BlockPos, glowColor: Int): Display.BlockDisplay =
        markBlock(pos, world().getBlockState(pos), glowColor)

    fun markBlockFace(pos: BlockPos, face: Direction, glowColor: Int): Display.BlockDisplay =
        markBlockFace(pos, face, world().getBlockState(pos), glowColor)
}