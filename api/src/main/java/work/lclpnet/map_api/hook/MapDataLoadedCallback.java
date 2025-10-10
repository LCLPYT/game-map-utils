package work.lclpnet.map_api.hook;

import net.minecraft.server.world.ServerWorld;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;
import work.lclpnet.map_api.data.WorldData;

public interface MapDataLoadedCallback {

    Hook<MapDataLoadedCallback> HOOK = HookFactory.createArrayBacked(MapDataLoadedCallback.class, hooks -> (world, worldData) -> {
        for (var hook : hooks) {
            hook.onMapDataLoaded(world, worldData);
        }
    });

    void onMapDataLoaded(ServerWorld world, WorldData worldData);
}
