package work.lclpnet.map_api.visual

import net.minecraft.world.level.block.Blocks
import work.lclpnet.gaco.math.SplinePath
import work.lclpnet.gaco.scene.Object3d

fun displaySplinePath(path: SplinePath, visualizer: Visualizer): Pair<Removable, List<Object3d>> {
    val samples = path.keypoints.size * 15

    val markers = mutableListOf<Object3d>()
    val keypointMarkers = mutableListOf<Object3d>()

    for (keypoint in path.keypoints) {
        val keypointMarker = visualizer.marker(keypoint, Blocks.CONCRETE.orange().defaultBlockState(), 0xeeff00, 0.5)
        keypointMarkers.add(keypointMarker)
        markers.add(keypointMarker)
    }

    var start = path.keypoints.first()
    val step = 1.0 / (samples - 1)

    markers.add(visualizer.marker(start, Blocks.CONCRETE.yellow().defaultBlockState(), 0xeeff00, 0.2))

    for (i in 1..<samples) {
        val s = i * step

        val end = path.samplePosition(s)

        markers.add(visualizer.line(start, end, 0.1, Blocks.CONCRETE.yellow.defaultBlockState()))

        start = end
    }

    return Pair(
        Removable {
            for (marker in markers) {
                marker.detach()
            }

            markers.clear()
        },
        keypointMarkers
    )
}