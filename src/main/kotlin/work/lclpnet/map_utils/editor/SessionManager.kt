package work.lclpnet.map_utils.editor

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.registry.RegistryKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.World
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.ServerLifecycleHooks
import work.lclpnet.kibu.hook.player.PlayerConnectionHooks
import java.util.*

class SessionManager {

    private val sessions = mutableMapOf<UUID, MutableMap<RegistryKey<World>, Session>>()

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(ServerLifecycleHooks.SERVER_STOPPED, ServerLifecycleEvents.ServerStopped {
            clearSessions()
        })

        hooks.registerHook(PlayerConnectionHooks.QUIT, PlayerConnectionHooks.ServerPlayerAction {
            clearSession(it)
        })
    }

    @Synchronized
    fun getSession(player: ServerPlayerEntity): Session {
        return sessions.computeIfAbsent(player.uuid) { mutableMapOf() }
            .computeIfAbsent(player.world.registryKey) { Session() }
    }

    @Synchronized
    fun clearSession(player: ServerPlayerEntity) {
        sessions.remove(player.uuid)
    }

    @Synchronized
    fun clearSessions() {
        sessions.clear()
    }
}