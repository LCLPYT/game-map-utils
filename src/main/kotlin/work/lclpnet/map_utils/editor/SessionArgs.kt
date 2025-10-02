package work.lclpnet.map_utils.editor

import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.world.ServerWorld
import work.lclpnet.kibu.translate.Translations

class SessionArgs(
    val translations: Translations,
    val world: ServerWorld,
    val networkHandler: ServerPlayNetworkHandler,
) {
    fun player() = networkHandler.player!!
    fun server() = world.server
}