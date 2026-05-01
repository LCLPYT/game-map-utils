package work.lclpnet.map_utils.hook;

import net.minecraft.server.level.ServerPlayer;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface VirtualEntityAttackCallback {

    Hook<VirtualEntityAttackCallback> HOOK = HookFactory.createArrayBacked(VirtualEntityAttackCallback.class, hooks -> (player, entityId) -> {
        for (var hook : hooks) {
            hook.onAttack(player, entityId);
        }
    });

    void onAttack(ServerPlayer player, int entityId);
}
