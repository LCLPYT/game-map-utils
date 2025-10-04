package work.lclpnet.map_utils.data

import com.mojang.serialization.Codec
import net.minecraft.server.network.ServerPlayerEntity
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.editor.DataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.visual.Visualizer

interface Data<T> {

    fun id(): String

    fun codec(): Codec<T>

    fun createEditor(args: SessionArgs, visualizer: Visualizer): DataEditor<T>

    fun display(
        value: T,
        visualizer: Visualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        id: String,
        propertyId: String? = null
    ): Removable
}

