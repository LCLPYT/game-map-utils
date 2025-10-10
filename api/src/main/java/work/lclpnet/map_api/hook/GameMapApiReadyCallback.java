package work.lclpnet.map_api.hook;

import net.minecraft.server.MinecraftServer;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;
import work.lclpnet.map_api.GameMapApi;

public interface GameMapApiReadyCallback {

    Hook<GameMapApiReadyCallback> HOOK = HookFactory.createArrayBacked(GameMapApiReadyCallback.class, hooks -> (api, server) -> {
        for (var hook : hooks) {
            hook.onReady(api, server);
        }
    });

    void onReady(GameMapApi api, MinecraftServer server);
}
