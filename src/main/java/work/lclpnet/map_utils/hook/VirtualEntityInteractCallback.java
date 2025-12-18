package work.lclpnet.map_utils.hook;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface VirtualEntityInteractCallback {

    Hook<VirtualEntityInteractCallback> HOOK = HookFactory.createArrayBacked(VirtualEntityInteractCallback.class, hooks -> (player, entityId) -> {
        for (var hook : hooks) {
            var handler = hook.provideHandler(player, entityId);

            if (handler != null) {
                return handler;
            }
        }

        return null;
    });

    @Nullable
    ServerboundInteractPacket.Handler provideHandler(ServerPlayer player, int entityId);
}
