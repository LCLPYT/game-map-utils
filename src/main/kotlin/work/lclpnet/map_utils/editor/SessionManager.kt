package work.lclpnet.map_utils.editor

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.minecraft.registry.RegistryKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.World
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.ServerLifecycleHooks
import work.lclpnet.kibu.hook.entity.ServerEntityWorldChangeHooks
import work.lclpnet.kibu.hook.player.PlayerConnectionHooks
import work.lclpnet.kibu.hook.world.ServerWorldHooks
import work.lclpnet.kibu.scheduler.KibuScheduling
import work.lclpnet.kibu.scheduler.api.Scheduler
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.LOGGER
import work.lclpnet.map_api.data.DataManager
import java.util.*

class SessionManager(val translations: Translations, val dataManager: DataManager) {

    private val sessions = mutableMapOf<UUID, MutableMap<RegistryKey<World>, Session>>()
    private val worldSessions = mutableMapOf<RegistryKey<World>, WorldSession>()

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(ServerLifecycleHooks.SERVER_STOPPING, ServerLifecycleEvents.ServerStopping {
            clearSessions()
            clearWorldSessions()
        })

        hooks.registerHook(PlayerConnectionHooks.QUIT, PlayerConnectionHooks.ServerPlayerAction {
            clearSession(it)
        })

        hooks.registerHook(ServerWorldHooks.UNLOAD, ServerWorldEvents.Unload { _, world ->
            clearWorldSession(world)
        })

        hooks.registerHook(
            ServerEntityWorldChangeHooks.AFTER_PLAYER_CHANGE_WORLD,
            ServerEntityWorldChangeEvents.AfterPlayerChange { entity, origin, destination ->
                optSession(entity, origin)?.deactivateEditor()
                optSession(entity, destination)?.reactivateEditor()
            }
        )
    }

    @Synchronized
    private fun clearWorldSessions() {
        for (worldSession in worldSessions.values) {
           worldSession.destroy()
        }

        worldSessions.clear()
    }

    @Synchronized
    private fun clearWorldSession(world: ServerWorld) {
        val worldSession = worldSessions.remove(world.registryKey) ?: return

        worldSession.destroy()
    }

    fun optSession(player: ServerPlayerEntity) = optSession(player, player.entityWorld)

    fun optSession(player: ServerPlayerEntity, world: ServerWorld): Session? {
        val playerSessions = sessions[player.uuid] ?: return null

        return playerSessions[world.registryKey]
    }

    @Synchronized
    fun getSession(player: ServerPlayerEntity): Session {
        val world = player.entityWorld

        return sessions.computeIfAbsent(player.uuid) { mutableMapOf() }.computeIfAbsent(world.registryKey) {
            val worldData = getWorldSession(world)

            Session(SessionArgs(translations, world, player.networkHandler), worldData.dynamicEntityManager, dataManager).also { it.init() }
        }
    }

    fun getWorldSession(world: ServerWorld): WorldSession {
        return worldSessions.computeIfAbsent(world.registryKey) {
            WorldSession(DynamicEntityManager(world)).also { it.init() }
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

class WorldSession(val dynamicEntityManager: DynamicEntityManager) {

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

    fun destroy() {
        dynamicEntityManager.clear()
        hooks.unload()
        KibuScheduling.getRootScheduler().removeChild(scheduler)
    }
}