package work.lclpnet.map_utils.data

import com.mojang.serialization.Codec
import java.util.function.Function

class WorldData(private val dataMap: MutableMap<String, DataInstance<*>> = mutableMapOf()) {

    @Synchronized
    operator fun set(propertyId: String, instance: DataInstance<*>) {
        dataMap[propertyId] = instance
    }

    operator fun get(propertyId: String): DataInstance<*>? {
        return dataMap[propertyId]
    }

    @Synchronized
    fun copyFrom(source: WorldData) {
        dataMap.clear()
        dataMap.putAll(source.dataMap)
    }

    companion object {
        @JvmField
        val CODEC: Codec<WorldData> = Codec.unboundedMap(Codec.STRING, DataInstance.CODEC).xmap(
            Function { WorldData(it) },
            Function { it.dataMap }
        )
    }
}