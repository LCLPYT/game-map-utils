package work.lclpnet.map_utils.schema

import net.minecraft.server.level.ServerLevel
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.data.WorldData
import work.lclpnet.map_api.hook.MapDataLoadedCallback
import work.lclpnet.map_api.schema.MapSchema
import java.util.concurrent.CompletableFuture

class SchemaManager(val schemaLoader: SchemaLoader, val dataManager: DataManager) {

    val schemas = mutableMapOf<String, MapSchema>()

    fun init(hooks: HookRegistrar) {
        reloadSchemasBlocking()

        hooks.registerHook(MapDataLoadedCallback.HOOK, MapDataLoadedCallback { world, data ->
            loadDefaults(world, data)
        })
    }

    fun reloadSchemas(): CompletableFuture<Void> = CompletableFuture.runAsync { reloadSchemasBlocking() }

    fun reloadSchemasBlocking() {
        val schemas = schemaLoader.loadAll()

        this.schemas.clear()
        this.schemas.putAll(schemas)
    }

    fun getSchema(world: ServerLevel): MapSchema? =
        schemas[dataManager.getWorldData(world).schemaId]

    fun setSchema(world: ServerLevel, schema: MapSchema?) {
        dataManager.getWorldData(world).schemaId = schema?.id
    }

    fun loadDefaults(world: ServerLevel, data: WorldData) {
        val schema = getSchema(world)

        if (schema != null) {
            data.loadDefaults(schema)
        }

        dataManager.save(world)
    }
}
