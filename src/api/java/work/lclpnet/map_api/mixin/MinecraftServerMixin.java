package work.lclpnet.map_api.mixin;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.map_api.GameMapApi;
import work.lclpnet.map_api.type.GameMapApiMinecraftServer;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements GameMapApiMinecraftServer {

    @Unique @Nullable
    private GameMapApi api = null;

    @Override
    public void gameMapApi$set(GameMapApi api) {
        this.api = api;
    }

    @Override
    public GameMapApi gameMapApi$get() {
        return api;
    }
}
