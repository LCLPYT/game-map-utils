package work.lclpnet.map_api.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.JsonOps
import com.mojang.serialization.Lifecycle
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
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

    val worldData = mutableMapOf<ResourceKey<Level>, WorldData>()
    val gson = Gson()
    private val fileLock = emptyArray<Unit>()
    private val worldDataFutures = mutableMapOf<ResourceKey<Level>, CompletableFuture<WorldData>>()
    private val loading = mutableSetOf<ResourceKey<Level>>()

    fun init(hooks: HookContainer) {
        hooks.registerHook(ServerWorldHooks.LOAD, ServerWorldEvents.Load { _, world ->
            load(world)
        })

        hooks.registerHook(ServerWorldHooks.UNLOAD, ServerWorldEvents.Unload { _, world ->
            unload(world)

            val futures: List<CompletableFuture<WorldData>>

            synchronized(this) {
                futures = worldDataFutures.values.toList()
                worldDataFutures.clear()
            }

            futures.forEach { it.completeExceptionally(IllegalStateException("World unloaded but future was never completed / cleared")) }
        })
    }

    fun <T> setData(world: ServerLevel, propertyId: String, data: Data<T>, value: T) {
        setDataInstance(world, propertyId, DataInstance(data, value, null))
    }

    fun <T> setDataInstance(world: ServerLevel, propertyId: String, dataInstance: DataInstance<T>) {
        getWorldData(world)[propertyId] = dataInstance
    }

    fun removeData(world: ServerLevel, propertyId: String) {
        getWorldData(world).remove(propertyId)
    }

    fun hasData(world: ServerLevel, propertyId: String) = getWorldData(world).has(propertyId)

    fun <T> getDataInstance(world: ServerLevel, propertyId: String, data: Data<T>): DataInstance<T>? {
        val instance = getWorldData(world)[propertyId] ?: return null

        if (instance.data == data) {
            @Suppress("UNCHECKED_CAST")
            return instance as DataInstance<T>
        }

        return null
    }

    @JvmOverloads
    fun <T> getData(world: ServerLevel, propertyId: String, data: Data<T>, default: T? = null): T? {
        val instance = getDataInstance(world, propertyId, data) ?: return default

        return instance.value
    }

    @Synchronized
    fun load(world: ServerLevel): CompletableFuture<WorldData> {
        val key = world.dimension()

        if (!loading.add(key)) {
            return requireNotNull(worldDataFutures[key]) { "Expected world data future for $key to exist" }
        }

        logger.debug("Loading world data for {} asynchronously...", key)

        val future = CompletableFuture.supplyAsync {
            val data = loadBlocking(world)
            setAll(world, data)
        }.whenComplete { data, err ->
            if (err != null) {
                logger.error("Failed to load map data of world ${key.identifier()}", err)
            } else {
                world.server?.execute {
                    MapDataLoadedCallback.HOOK.invoker().onMapDataLoaded(world, data)
                }
            }
        }

        val prev = worldDataFutures[key]

        if (prev != null) {
            logger.debug("World data future already exists for {}, merging...", key)
        }

        worldDataFutures[key] = future.whenComplete { data, err ->
            logger.debug("World data future for {} is complete", key)

            if (err != null) prev?.completeExceptionally(err)
            else prev?.complete(data)

            synchronized(this) {
                worldDataFutures.remove(key)
                loading.remove(key)
            }
        }

        return future
    }

    fun load(path: Path): CompletableFuture<WorldData> = CompletableFuture.supplyAsync {
        loadBlocking(path)
    }

    @Synchronized
    private fun unload(world: ServerLevel) {
        worldData.remove(world.dimension())
    }

    fun save(world: ServerLevel): CompletableFuture<Void> = CompletableFuture.runAsync {
        saveBlocking(world)
    }.whenComplete { _, err ->
        if (err != null) {
            logger.error("Failed to save map data of world ${world.dimension().identifier()}", err)
        }
    }

    fun save(worldData: WorldData, path: Path): CompletableFuture<Void> = CompletableFuture.runAsync {
        saveBlocking(worldData, path)
    }

    @Synchronized
    private fun setAll(world: ServerLevel, source: WorldData): WorldData {
        val worldData = getWorldData(world)
        worldData.copyFrom(source)

        worldData.schemaId = source.schemaId

        return worldData
    }

    private fun loadBlocking(world: ServerLevel): WorldData {
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

    private fun saveBlocking(world: ServerLevel) {
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

    fun dataFile(world: ServerLevel): Path {
        val session = (world.server as MinecraftServerAccessor).storageSource
        val worldDir = session.getDimensionPath(world.dimension())

        return worldDir.resolve("data").resolve(WORLD_DATA_FILENAME)
    }

    @Synchronized
    fun getWorldData(world: ServerLevel) = worldData.computeIfAbsent(world.dimension()) { WorldData() }

    @Synchronized
    fun awaitWorldData(worldKey: ResourceKey<Level>): CompletableFuture<WorldData> {
        val data = worldData[worldKey]

        if (data != null) {
            logger.debug("World data already exists for world {}", worldKey)
            return CompletableFuture.completedFuture(data)
        }

        logger.debug("No world data exists for {}, deferring completion...", worldKey)
        return worldDataFutures.computeIfAbsent(worldKey) { CompletableFuture() }
    }

    companion object {
        @JvmField
        val DATA_TYPES = listOf<Data<*>>(
            BlockPosData,
            BlockBoxData,
            PositionData,
            CheckpointData,
            PositionedBlockSetData,
            SplinePathData,
            BlockFaceData,
        ).associateBy { it.id() }
    }
}