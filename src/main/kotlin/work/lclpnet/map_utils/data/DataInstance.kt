package work.lclpnet.map_utils.data

import com.mojang.serialization.Codec
import net.minecraft.server.network.ServerPlayerEntity
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.editor.DataEditor
import work.lclpnet.map_utils.editor.EditorVisualizer
import work.lclpnet.map_utils.editor.Session
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

    fun restore(session: Session): DataEditor<T> {
        val editor = session.createEditor(data)

        editor.load(value)

        return editor
    }

    fun display(
        visualizer: EditorVisualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        propertyId: String
    ) = data.display(value, visualizer, player, translations, propertyId)

    companion object {
        @JvmField
        val CODEC: Codec<DataInstance<*>> = DATA_CODEC.dispatch("type", Function { it.data }, Function { data ->
            dataInstanceMapCodec(data)
        })
    }
}


