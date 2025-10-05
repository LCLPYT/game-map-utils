package work.lclpnet.map_api.schema

class MapSchema(
    val name: String,
    val properties: Map<String, DataDefinition<*>>
) {
}