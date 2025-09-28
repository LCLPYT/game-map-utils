package work.lclpnet.map_utils.editor

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.registry.RegistryKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.World
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.ServerLifecycleHooks
import work.lclpnet.kibu.hook.player.PlayerConnectionHooks
import work.lclpnet.kibu.scheduler.KibuScheduling
import work.lclpnet.kibu.scheduler.api.Scheduler
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.LOGGER
import java.util.*

class SessionManager(val translations: Translations) {

    private val sessions = mutableMapOf<UUID, MutableMap<RegistryKey<World>, Session>>()
    private val worldData = mutableMapOf<RegistryKey<World>, WorldData>()

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(ServerLifecycleHooks.SERVER_STOPPING, ServerLifecycleEvents.ServerStopping {
            clearSessions()
        })

        hooks.registerHook(PlayerConnectionHooks.QUIT, PlayerConnectionHooks.ServerPlayerAction {
            clearSession(it)
        })
    }

    fun optSession(player: ServerPlayerEntity): Session? {
        val playerSessions = sessions[player.uuid] ?: return null

        return playerSessions[player.world.registryKey]
    }

    @Synchronized
    fun getSession(player: ServerPlayerEntity): Session {
        val world = player.world

        return sessions.computeIfAbsent(player.uuid) { mutableMapOf() }.computeIfAbsent(world.registryKey) {
            val worldData = getWorldData(world)

            Session(SessionArgs(translations, world, player.networkHandler, worldData.dynamicEntityManager)).also { it.init() }
        }
    }

    fun getWorldData(world: ServerWorld): WorldData {
        return worldData.computeIfAbsent(world.registryKey) {
            WorldData(DynamicEntityManager(world)).also { it.init() }
        }
    }

    fun isEditing(player: ServerPlayerEntity): Boolean {
        return optSession(player)?.editor != null
    }

    @Synchronized
    fun clearSession(player: ServerPlayerEntity) {
        val playerSessions = sessions.remove(player.uuid) ?: return

        for ((_, session) in playerSessions) {
            session.destroy()
        }
    }

    @Synchronized
    fun clearSessions() {
        for ((_, playerSessions) in sessions) {
            for ((_, session) in playerSessions) {
                session.destroy()
            }
        }

        sessions.clear()
    }
}

class WorldData(val dynamicEntityManager: DynamicEntityManager) {

    private val hooks = HookContainer()
    private var scheduler: Scheduler? = null

    fun init() {
        var scheduler = this.scheduler

        if (scheduler == null) {
            scheduler = Scheduler(LOGGER)
            this.scheduler = scheduler
        }

        dynamicEntityManager.init(scheduler, hooks)
        KibuScheduling.getRootScheduler().addChild(scheduler)
    }

    fun reset() {
        dynamicEntityManager.clear()
        hooks.unload()
        KibuScheduling.getRootScheduler().removeChild(scheduler)
    }
}