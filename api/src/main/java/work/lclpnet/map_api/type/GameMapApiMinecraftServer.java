package work.lclpnet.map_api.type;

import work.lclpnet.map_api.GameMapApi;

public interface GameMapApiMinecraftServer {

    void gameMapApi$set(GameMapApi api);

    GameMapApi gameMapApi$get();
}
