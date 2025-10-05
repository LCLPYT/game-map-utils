package work.lclpnet.map_api.data

import com.mojang.serialization.Codec
import net.minecraft.server.network.ServerPlayerEntity
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer

interface Data<T> {

    fun id(): String

    fun codec(): Codec<T>

    fun display(
        value: T,
        visualizer: Visualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        id: String,
        propertyId: String? = null
    ): Removable
}