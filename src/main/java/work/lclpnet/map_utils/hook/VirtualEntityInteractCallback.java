package work.lclpnet.map_utils.hook;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface VirtualEntityInteractCallback {

    Hook<VirtualEntityInteractCallback> HOOK = HookFactory.createArrayBacked(VirtualEntityInteractCallback.class, hooks -> (player, entityId, hand, pos) -> {
        for (var hook : hooks) {
            hook.onInteract(player, entityId, hand, pos);
        }
    });

    void onInteract(ServerPlayer player, int entityId, InteractionHand hand, Vec3 pos);
}
