package work.lclpnet.map_api.data

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.server.network.ServerPlayerEntity
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.visual.Visualizer
import java.util.*
import java.util.function.Function

private fun <T> mapCodec(data: Data<T>): MapCodec<DataInstance<T>> = RecordCodecBuilder.mapCodec { instance ->
    instance.group(
        data.codec().fieldOf("value").forGetter { it.value },
        Codec.STRING.optionalFieldOf("role").forGetter { Optional.ofNullable(it.role) }
    ).apply(instance) { value, role ->
        DataInstance(data, value, role.orElse(null))
    }
}

data class DataInstance<T>(
    val data: Data<T>,
    val value: T,
    val role: String?
) {
    fun display(
        visualizer: Visualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        propertyId: String
    ) = data.display(value, visualizer, player, translations, data.id(), propertyId)

    companion object {
        @JvmField
        val CODEC: Codec<DataInstance<*>> = DATA_CODEC.dispatch("type", Function { it.data }, Function { data ->
            mapCodec(data)
        })
    }
}


