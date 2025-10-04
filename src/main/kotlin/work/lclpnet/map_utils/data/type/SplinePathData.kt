package work.lclpnet.map_utils.data.type

import com.mojang.serialization.Codec
import net.minecraft.block.Blocks
import net.minecraft.server.network.ServerPlayerEntity
import work.lclpnet.gaco.math.SplinePath
import work.lclpnet.gaco.scene.Object3d
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.Data
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.editor.type.SplinePathEditor
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.visual.Visualizer

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
    ): Removable {
        val samples = value.keypoints.size * 15

        val markers = mutableListOf<Object3d>()

        for (keypoint in value.keypoints) {
            markers.add(visualizer.marker(keypoint, Blocks.ORANGE_CONCRETE.defaultState, 0xeeff00, 0.5))
        }

        var start = value.keypoints.first()
        val step = 1.0 / (samples - 1)

        markers.add(visualizer.marker(start, Blocks.YELLOW_CONCRETE.defaultState, 0xeeff00, 0.2))

        for (i in 1..<samples) {
            val s = i * step

            val end = value.samplePosition(s)

            markers.add(visualizer.line(start, end, 0.1, Blocks.YELLOW_CONCRETE.defaultState))

            start = end
        }

        return Removable {
            for (marker in markers) {
                marker.detach()
            }

            markers.clear()
        }
    }
}