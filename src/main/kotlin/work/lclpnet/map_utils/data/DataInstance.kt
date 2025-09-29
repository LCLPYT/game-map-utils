package work.lclpnet.map_utils.data

import com.mojang.serialization.Codec
import java.util.function.Function

private fun <T> makeDataInstanceUnsafe(data: Data<T>, value: Any?): DataInstance<T> {
    @Suppress("UNCHECKED_CAST")
    val cast = value as T

    return DataInstance(data, cast)
}

private fun <T> dataInstanceMapCodec(data: Data<T>) = data.codec().fieldOf("value").xmap(
    Function { value ->
        makeDataInstanceUnsafe(data, value)
    },
    Function {
        it.value
    }
)

data class DataInstance<T>(val data: Data<T>, val value: T) {
    companion object {
        @JvmField
        val CODEC: Codec<DataInstance<*>> = DATA_CODEC.dispatch("type", Function { it.data }, Function { data ->
            dataInstanceMapCodec(data)
        })
    }
}


