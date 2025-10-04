package work.lclpnet.map_utils.data.type

import com.mojang.serialization.Codec
import net.minecraft.server.network.ServerPlayerEntity
import work.lclpnet.gaco.math.SplinePath
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.Data
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.editor.type.SplinePathEditor
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.visual.Visualizer
import work.lclpnet.map_utils.visual.displaySplinePath

object SplinePathData : Data<SplinePath> {

    override fun id() = "spline_path"

    override fun codec(): Codec<SplinePath> = SplinePath.CODEC

    override fun createEditor(args: SessionArgs, visualizer: Visualizer) = SplinePathEditor(args, visualizer)

    override fun display(
        value: SplinePath,
        visualizer: Visualizer,
        player: ServerPlayerEntity,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable = displaySplinePath(value, visualizer).first
}