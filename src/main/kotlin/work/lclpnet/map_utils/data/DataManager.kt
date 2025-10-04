package work.lclpnet.map_utils.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.JsonOps
import com.mojang.serialization.Lifecycle
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.minecraft.registry.RegistryKey
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.World
import org.slf4j.Logger
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.hook.world.ServerWorldHooks
import work.lclpnet.map_utils.data.type.BlockBoxData
import work.lclpnet.map_utils.data.type.CheckpointData
import work.lclpnet.map_utils.data.type.PositionData
import work.lclpnet.map_utils.data.type.PositionedBlockSetData
import work.lclpnet.map_utils.mixin.MinecraftServerAccessor
import work.lclpnet.map_utils.util.toPrettyString
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.function.Function

const val WORLD_DATA_FILENAME = "gaco-map.json"

val DATA_TYPES = listOf<Data<*>>(
    BlockBoxData,
    PositionData,
    PositionedBlockSetData,
    CheckpointData,
).associateBy { it.id() }

val DATA_CODEC: Codec<Data<*>> = Codec.STRING.comapFlatMap(
    Function { id ->
        var type = DATA_TYPES[id]

        if (type == null) {
            DataResult.error { "Unknown shape type: $id" }
        } else {
            DataResult.success(type, Lifecycle.stable())
        }
    },
    Function<Data<*>, String> {
        it.id()
    }
)

class DataManager(val logger: Logger) {

    val worldData = mutableMapOf<RegistryKey<World>, WorldData>()
    val gson = Gson()
    private val lock = emptyArray<Unit>()

    fun init(hooks: HookContainer) {
        hooks.registerHook(ServerWorldHooks.LOAD, ServerWorldEvents.Load { _, world ->
            load(world)
        })
    }

    fun <T> setData(world: ServerWorld, propertyId: String, data: Data<T>, value: T) {
        getWorldData(world)[propertyId] = DataInstance(data, value)
    }

    fun removeData(world: ServerWorld, propertyId: String) {
        getWorldData(world).remove(propertyId)
    }

    fun hasData(world: ServerWorld, propertyId: String) = getWorldData(world).has(propertyId)

    @JvmOverloads
    fun <T> getData(world: ServerWorld, propertyId: String, data: Data<T>, default: T? = null): T? {
        val instance = getWorldData(world)[propertyId] ?: return default

        if (instance.data == data) {
            @Suppress("UNCHECKED_CAST")
            return instance.value as T
        }

        return default
    }

    fun load(world: ServerWorld): CompletableFuture<WorldData> = CompletableFuture.supplyAsync {
        val data = loadBlocking(world)
        setAll(world, data)
    }.whenComplete { _, err ->
        if (err != null) {
            logger.error("Failed to load map data of world ${world.registryKey.value}", err)
        }
    }

    fun save(world: ServerWorld): CompletableFuture<Void> = CompletableFuture.runAsync {
        saveBlocking(world)
    }.whenComplete { _, err ->
        if (err != null) {
            logger.error("Failed to save map data of world ${world.registryKey.value}", err)
        }
    }

    private fun setAll(world: ServerWorld, source: WorldData): WorldData {
        val worldData = getWorldData(world)
        worldData.copyFrom(source)
        return worldData
    }

    private fun loadBlocking(world: ServerWorld): WorldData {
        val path = dataFile(world)

        if (!Files.isRegularFile(path)) {
            return WorldData()
        }

        val content: String

        synchronized(lock) {
            content = Files.readString(path, StandardCharsets.UTF_8)
        }

        val json = gson.fromJson(content, JsonElement::class.java)

        return WorldData.CODEC.decode(JsonOps.INSTANCE, json)
            .resultOrPartial { logger.error("Failed to decode world data of world ${world.registryKey.value} from json: $it") }
            .map { it.first }
            .orElseGet { WorldData() }
    }

    private fun saveBlocking(world: ServerWorld) {
        val worldData = getWorldData(world)

        WorldData.CODEC.encodeStart(JsonOps.INSTANCE, worldData)
            .resultOrPartial { logger.error("Failed to encode world data of world ${world.registryKey.value} to json: $it") }
            .ifPresent { json -> synchronized(lock) {
                Files.writeString(dataFile(world), json.toPrettyString(), StandardCharsets.UTF_8)
            }}
    }

    fun dataFile(world: ServerWorld): Path {
        val session = (world.server as MinecraftServerAccessor).getSession()
        val worldDir = session.getWorldDirectory(world.registryKey)

        return worldDir.resolve("data").resolve(WORLD_DATA_FILENAME)
    }
    @Synchronized
    fun getWorldData(world: ServerWorld) = worldData.computeIfAbsent(world.registryKey) { WorldData() }
}