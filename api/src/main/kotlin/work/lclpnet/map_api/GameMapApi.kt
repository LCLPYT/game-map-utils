package work.lclpnet.map_api

import net.minecraft.server.MinecraftServer
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.type.GameMapApiMinecraftServer

class GameMapApi(val dataManager: DataManager) {

    companion object {
        fun get(server: MinecraftServer): GameMapApi {
            return (server as GameMapApiMinecraftServer).`gameMapApi$get`()
        }
    }
}