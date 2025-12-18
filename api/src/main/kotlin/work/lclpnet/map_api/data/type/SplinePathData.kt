package work.lclpnet.map_api.data.type

import com.mojang.serialization.Codec
import net.minecraft.server.level.ServerPlayer
import work.lclpnet.gaco.math.SplinePath
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_api.visual.displaySplinePath

object SplinePathData : Data<SplinePath> {

    override fun id() = "spline_path"

    override fun codec(): Codec<SplinePath> = SplinePath.CODEC

    override fun type() = SplinePath::class.java

    override fun display(
        value: SplinePath,
        visualizer: Visualizer,
        player: ServerPlayer,
        translations: Translations,
        id: String,
        propertyId: String?
    ): Removable = displaySplinePath(value, visualizer).first
}