package work.lclpnet.map_api.schema

class MapSchema(
    val id: String,
    val name: String,
    val properties: Map<String, DataDefinition<*, *>>
)