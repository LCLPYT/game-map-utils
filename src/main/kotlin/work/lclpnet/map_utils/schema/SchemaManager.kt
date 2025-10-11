package work.lclpnet.map_utils.schema

import net.minecraft.server.world.ServerWorld
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.data.WorldData
import work.lclpnet.map_api.hook.MapDataLoadedCallback
import work.lclpnet.map_api.schema.MapSchema

class SchemaManager(val schemas: Map<String, MapSchema>, val dataManager: DataManager) {

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(MapDataLoadedCallback.HOOK, MapDataLoadedCallback { world, data ->
            loadDefaults(world, data)
        })
    }

    fun getSchema(world: ServerWorld): MapSchema? =
        schemas[dataManager.getWorldData(world).schemaId]

    fun setSchema(world: ServerWorld, schema: MapSchema?) {
        dataManager.getWorldData(world).schemaId = schema?.id
    }

    fun loadDefaults(world: ServerWorld, data: WorldData) {
        val schema = getSchema(world)

        if (schema != null) {
            data.loadDefaults(schema)
        }

        dataManager.save(world)
    }
}
