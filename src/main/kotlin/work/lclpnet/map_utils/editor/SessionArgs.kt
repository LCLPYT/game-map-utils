package work.lclpnet.map_utils.editor

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.network.ServerGamePacketListenerImpl
import work.lclpnet.kibu.translate.Translations

class SessionArgs(
    val translations: Translations,
    val world: ServerLevel,
    val networkHandler: ServerGamePacketListenerImpl,
) {
    fun player() = networkHandler.player
    fun server() = world.server
}