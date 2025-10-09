package work.lclpnet.map_utils.schema

import net.minecraft.server.world.ServerWorld
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.schema.MapSchema

class SchemaManager(val schemas: Map<String, MapSchema>, val dataManager: DataManager) {

    fun getSchema(world: ServerWorld): MapSchema? =
        schemas[dataManager.getWorldData(world).schemaId]

    fun setSchema(world: ServerWorld, schema: MapSchema) {
        dataManager.getWorldData(world).schemaId = schema.id
        dataManager.save(world)
    }
}
