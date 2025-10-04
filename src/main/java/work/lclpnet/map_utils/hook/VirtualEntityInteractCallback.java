package work.lclpnet.map_utils.hook;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
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
    PlayerInteractEntityC2SPacket.Handler provideHandler(ServerPlayerEntity player, int entityId);
}
