package work.lclpnet.map_api.data

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
import work.lclpnet.map_api.data.DataManager.Companion.DATA_TYPES
import work.lclpnet.map_api.data.type.*
import work.lclpnet.map_api.hook.MapDataLoadedCallback
import work.lclpnet.map_api.mixin.MinecraftServerAccessor
import work.lclpnet.map_api.util.toPrettyString
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

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

const val WORLD_DATA_FILENAME = "gaco-map.json"

class DataManager(val logger: Logger) {

    val worldData = mutableMapOf<RegistryKey<World>, WorldData>()
    val gson = Gson()
    private val fileLock = emptyArray<Unit>()

    fun init(hooks: HookContainer) {
        hooks.registerHook(ServerWorldHooks.LOAD, ServerWorldEvents.Load { _, world ->
            load(world)
        })

        hooks.registerHook(ServerWorldHooks.UNLOAD, ServerWorldEvents.Unload { _, world ->
            unload(world)
        })
    }

    fun <T> setData(world: ServerWorld, propertyId: String, data: Data<T>, value: T) {
        setDataInstance(world, propertyId, DataInstance(data, value, null))
    }

    fun <T> setDataInstance(world: ServerWorld, propertyId: String, dataInstance: DataInstance<T>) {
        getWorldData(world)[propertyId] = dataInstance
    }

    fun removeData(world: ServerWorld, propertyId: String) {
        getWorldData(world).remove(propertyId)
    }

    fun hasData(world: ServerWorld, propertyId: String) = getWorldData(world).has(propertyId)

    fun <T> getDataInstance(world: ServerWorld, propertyId: String, data: Data<T>): DataInstance<T>? {
        val instance = getWorldData(world)[propertyId] ?: return null

        if (instance.data == data) {
            @Suppress("UNCHECKED_CAST")
            return instance as DataInstance<T>
        }

        return null
    }

    @JvmOverloads
    fun <T> getData(world: ServerWorld, propertyId: String, data: Data<T>, default: T? = null): T? {
        val instance = getDataInstance(world, propertyId, data) ?: return default

        return instance.value
    }

    fun load(world: ServerWorld): CompletableFuture<WorldData> = CompletableFuture.supplyAsync {
        val data = loadBlocking(world)
        setAll(world, data)
    }.whenComplete { data, err ->
        if (err != null) {
            logger.error("Failed to load map data of world ${world.registryKey.value}", err)
        } else {
            MapDataLoadedCallback.HOOK.invoker().onMapDataLoaded(world, data)
        }
    }

    @Synchronized
    private fun unload(world: ServerWorld) {
        worldData.remove(world.registryKey)
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

        worldData.schemaId = source.schemaId

        return worldData
    }

    private fun loadBlocking(world: ServerWorld): WorldData {
        val path = dataFile(world)

        return loadBlocking(path)
    }

    fun loadBlocking(path: Path): WorldData {
        if (!path.isRegularFile()) {
            return WorldData()
        }

        val content: String

        synchronized(fileLock) {
            content = path.readText(StandardCharsets.UTF_8)
        }

        val json = gson.fromJson(content, JsonElement::class.java)

        return WorldData.CODEC.decode(JsonOps.INSTANCE, json)
            .resultOrPartial { logger.error("Failed to decode world data from $path: $it") }
            .map { it.first }
            .orElseGet { WorldData() }
    }

    private fun saveBlocking(world: ServerWorld) {
        val worldData = getWorldData(world)
        val path = dataFile(world)

        saveBlocking(worldData, path)
    }

    fun saveBlocking(worldData: WorldData, path: Path) {
        WorldData.CODEC.encodeStart(JsonOps.INSTANCE, worldData)
            .resultOrPartial { logger.error("Failed to encode world data to $path: $it") }
            .ifPresent { json ->
                synchronized(fileLock) {
                    path.writeText(json.toPrettyString(), StandardCharsets.UTF_8)
                }
            }
    }

    fun dataFile(world: ServerWorld): Path {
        val session = (world.server as MinecraftServerAccessor).getSession()
        val worldDir = session.getWorldDirectory(world.registryKey)

        return worldDir.resolve("data").resolve(WORLD_DATA_FILENAME)
    }

    @Synchronized
    fun getWorldData(world: ServerWorld) = worldData.computeIfAbsent(world.registryKey) { WorldData() }

    companion object {
        val DATA_TYPES = listOf<Data<*>>(
            BlockPosData,
            BlockBoxData,
            PositionData,
            CheckpointData,
            PositionedBlockSetData,
            SplinePathData,
        ).associateBy { it.id() }
    }
}