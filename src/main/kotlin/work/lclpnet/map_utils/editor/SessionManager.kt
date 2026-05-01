package work.lclpnet.map_utils.editor

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.ServerLifecycleHooks
import work.lclpnet.kibu.hook.entity.ServerEntityLevelChangeHooks
import work.lclpnet.kibu.hook.level.ServerLevelHooks
import work.lclpnet.kibu.hook.player.PlayerConnectionHooks
import work.lclpnet.kibu.scheduler.KibuScheduling
import work.lclpnet.kibu.scheduler.api.Scheduler
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.LOGGER
import work.lclpnet.map_api.data.DataManager
import java.util.*

class SessionManager(val translations: Translations, val dataManager: DataManager) {

    private val sessions = mutableMapOf<UUID, MutableMap<ResourceKey<Level>, Session>>()
    private val worldSessions = mutableMapOf<ResourceKey<Level>, WorldSession>()

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(ServerLifecycleHooks.SERVER_STOPPING, ServerLifecycleEvents.ServerStopping {
            clearSessions()
            clearWorldSessions()
        })

        hooks.registerHook(PlayerConnectionHooks.QUIT, PlayerConnectionHooks.ServerPlayerAction {
            clearSession(it)
        })

        hooks.registerHook(ServerLevelHooks.UNLOAD, ServerLevelEvents.Unload { _, world ->
            clearWorldSession(world)
        })

        hooks.registerHook(
            ServerEntityLevelChangeHooks.AFTER_PLAYER_CHANGE_LEVEL,
            ServerEntityLevelChangeEvents.AfterPlayerChange { entity, origin, destination ->
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
    private fun clearWorldSession(world: ServerLevel) {
        val worldSession = worldSessions.remove(world.dimension()) ?: return

        worldSession.destroy()
    }

    fun optSession(player: ServerPlayer) = optSession(player, player.level())

    fun optSession(player: ServerPlayer, world: ServerLevel): Session? {
        val playerSessions = sessions[player.uuid] ?: return null

        return playerSessions[world.dimension()]
    }

    @Synchronized
    fun getSession(player: ServerPlayer): Session {
        val world = player.level()

        return sessions.computeIfAbsent(player.uuid) { mutableMapOf() }.computeIfAbsent(world.dimension()) {
            val worldData = getWorldSession(world)

            Session(SessionArgs(translations, world, player.connection), worldData.dynamicEntityManager, dataManager).also { it.init() }
        }
    }

    fun getWorldSession(world: ServerLevel): WorldSession {
        return worldSessions.computeIfAbsent(world.dimension()) {
            WorldSession(DynamicEntityManager(world)).also { it.init() }
        }
    }

    fun isEditing(player: ServerPlayer): Boolean {
        return optSession(player)?.editor != null
    }

    @Synchronized
    fun clearSession(player: ServerPlayer) {
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