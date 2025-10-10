package work.lclpnet.map_api.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import work.lclpnet.map_api.schema.DataDefinition
import work.lclpnet.map_api.schema.ListDataDefinition
import work.lclpnet.map_api.schema.MapSchema
import work.lclpnet.map_api.schema.SingleDataDefinition

class WorldData(
    private val properties: MutableMap<String, DataInstance<*>> = mutableMapOf(),
    var schemaId: String? = null,
) {

    @Synchronized
    operator fun set(propertyId: String, instance: DataInstance<*>) {
        properties[propertyId] = instance
    }

    @Synchronized
    operator fun get(propertyId: String): DataInstance<*>? = properties[propertyId]

    @Synchronized
    fun copyFrom(source: WorldData) {
        properties.clear()
        properties.putAll(source.properties)
    }

    @Synchronized
    fun remove(propertyId: String): DataInstance<*>? = properties.remove(propertyId)

    @Synchronized
    fun has(propertyId: String) = properties.contains(propertyId)

    @Synchronized
    fun getIndex(propertyId: String): Int {
        val list = properties.entries.toMutableList()
        return list.indexOfFirst { it.key == propertyId }
    }

    @Synchronized
    fun setIndex(propertyId: String, index: Int) {
        if (index < 0 || index >= properties.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for size ${properties.size}")
        }

        val list = properties.entries.toMutableList()
        val oldIndex = list.indexOfFirst { it.key == propertyId }

        if (oldIndex == -1 || oldIndex == index) return

        val removed = list.removeAt(oldIndex)

        list.add(index, removed)

        properties.clear()

        for ((k, v) in list) {
            properties[k] = v
        }
    }

    @Synchronized
    fun properties(): Map<String, DataInstance<*>> = properties.toMap()

    @Synchronized
    fun <T> byRole(role: String?, data: Data<T>): List<DataInstance<T>> {
        return properties.values
            .filter { it.data == data }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as DataInstance<T>
            }
            .filter { it.role == role }
    }

    fun loadDefaults(schema: MapSchema) {
        for ((propertyId, definition) in schema.properties) {
            loadDefault(propertyId, definition)
        }
    }

    private fun <D, T> loadDefault(propertyId: String, definition: DataDefinition<D, T>) {
        when (definition) {
            is SingleDataDefinition<*> -> {
                if (has(propertyId)) return

                val instance = definition.makeDefaultInstance()

                if (instance != null) {
                    set(propertyId, instance)
                }
            }

            is ListDataDefinition<*> -> {
                if (byRole(definition.role, definition.data).isNotEmpty()) return

                val instances = definition.makeDefaultInstances() ?: return

                for (instance in instances) {
                    val id = uniqueId(propertyId)

                    set(id, instance)
                }
            }
        }
    }

    private fun uniqueId(prefix: String): String {
        var i = 1
        var id = "${prefix}_$i"

        while (has(id)) {
            i++
            id = "${prefix}_$i"
        }

        return id
    }

    companion object {
        @JvmField
        val PROPERTY_MAP_CODEC: Codec<MutableMap<String, DataInstance<*>>> = Codec.unboundedMap(Codec.STRING, DataInstance.CODEC)

        @JvmField
        val CODEC: Codec<WorldData> = RecordCodecBuilder.create { it ->
            it.group(
                PROPERTY_MAP_CODEC.fieldOf("properties").forGetter { it.properties },
                Codec.STRING.fieldOf("schemaId").forGetter { it.schemaId },
            ).apply(it) { properties, schemaId ->
                WorldData(properties, schemaId)
            }
        }
    }
}