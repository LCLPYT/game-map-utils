package work.lclpnet.map_api

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.hook.ServerLifecycleHooks
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.hook.GameMapApiReadyCallback
import work.lclpnet.map_api.type.GameMapApiMinecraftServer

const val MOD_ID = "game-map-api"
val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

fun init() {
    val hooks = HookContainer()

    ServerLifecycleHooks.SERVER_STARTING.register(ServerLifecycleEvents.ServerStarting { server ->
        val dataManager = DataManager(LOGGER)

        dataManager.init(hooks)

        val api = GameMapApi(dataManager)

        (server as GameMapApiMinecraftServer).`gameMapApi$set`(api)

        GameMapApiReadyCallback.HOOK.invoker().onReady(api, server)
    })

    ServerLifecycleHooks.SERVER_STOPPED.register { hooks.unload() }
}
