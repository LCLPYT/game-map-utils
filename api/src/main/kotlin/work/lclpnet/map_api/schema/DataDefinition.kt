package work.lclpnet.map_api.schema

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import work.lclpnet.map_api.data.DATA_CODEC
import work.lclpnet.map_api.data.Data
import java.util.function.Function

private fun <T> mapCodec(data: Data<T>): MapCodec<DataDefinition<T>> = RecordCodecBuilder.mapCodec { instance ->
    instance.group(
        data.codec().fieldOf("default").forGetter { it.default },
        Codec.STRING.optionalFieldOf("role", null).forGetter { it.role },
        Codec.BOOL.optionalFieldOf("optional", false).forGetter { it.optional }
    ).apply(instance) { value, role, optional ->
        DataDefinition(data, value, role, optional)
    }
}

class DataDefinition<T>(
    val data: Data<T>,
    val default: T,
    val role: String?,
    val optional: Boolean
) {
    companion object {
        @JvmField
        val CODEC: Codec<DataDefinition<*>> = DATA_CODEC.dispatch("type", Function { it.data }, Function { data ->
            mapCodec(data)
        })
    }
}