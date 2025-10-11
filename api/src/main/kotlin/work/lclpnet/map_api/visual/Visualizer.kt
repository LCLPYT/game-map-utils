package work.lclpnet.map_api.visual

import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i

interface Visualizer : SceneRenderer {

    fun world(): ServerWorld

    fun addEntity(entity: Entity)

    fun removeEntity(entity: Entity)

    fun markBlock(pos: Vec3i, state: BlockState, glowColor: Int): DisplayEntity.BlockDisplayEntity

    fun markBlock(pos: BlockPos, glowColor: Int): DisplayEntity.BlockDisplayEntity =
        markBlock(pos, world().getBlockState(pos), glowColor)
}