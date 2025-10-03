package work.lclpnet.map_utils.util

import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3i

interface Visualizer {

    fun world(): ServerWorld

    fun addEntity(entity: Entity)

    fun removeEntity(entity: Entity)

    fun markBlock(pos: Vec3i, state: BlockState, glowColor: Int): DisplayEntity.BlockDisplayEntity
}