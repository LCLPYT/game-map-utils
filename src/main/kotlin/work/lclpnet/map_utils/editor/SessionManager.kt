package work.lclpnet.map_utils.editor

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.registry.RegistryKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.World
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.ServerLifecycleHooks
import work.lclpnet.kibu.hook.player.PlayerConnectionHooks
import work.lclpnet.kibu.translate.Translations
import java.util.*

class SessionManager(val translations: Translations) {

    private val sessions = mutableMapOf<UUID, MutableMap<RegistryKey<World>, Session>>()

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
            Session(SessionArgs(translations, world, player.networkHandler)).also { it.init() }
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