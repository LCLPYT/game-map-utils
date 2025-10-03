package work.lclpnet.map_utils.data

import com.mojang.serialization.Codec
import work.lclpnet.map_utils.editor.DataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.editor.Visualizer

interface Data<T> {
    fun id(): String
    fun codec(): Codec<T>
    fun createEditor(args: SessionArgs, visualizer: Visualizer, propertyId: String?): DataEditor<T>
}

