package work.lclpnet.map_utils.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

class WorldData(private val properties: MutableMap<String, DataInstance<*>> = mutableMapOf()) {

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

    companion object {
        @JvmField
        val PROPERTY_MAP_CODEC: Codec<MutableMap<String, DataInstance<*>>> = Codec.unboundedMap(Codec.STRING, DataInstance.CODEC)

        @JvmField
        val CODEC: Codec<WorldData> = RecordCodecBuilder.create { it ->
            it.group(
                PROPERTY_MAP_CODEC.fieldOf("properties").forGetter { it.properties }
            ).apply(it) { properties ->
                WorldData(properties)
            }
        }
    }
}