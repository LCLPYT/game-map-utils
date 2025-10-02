package work.lclpnet.map_utils.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

class WorldData(private val properties: MutableMap<String, DataInstance<*>> = mutableMapOf()) {

    @Synchronized
    operator fun set(propertyId: String, instance: DataInstance<*>) {
        properties[propertyId] = instance
    }

    operator fun get(propertyId: String): DataInstance<*>? {
        return properties[propertyId]
    }

    @Synchronized
    fun copyFrom(source: WorldData) {
        properties.clear()
        properties.putAll(source.properties)
    }

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