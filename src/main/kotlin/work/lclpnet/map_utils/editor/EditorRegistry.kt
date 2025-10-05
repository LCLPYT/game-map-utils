package work.lclpnet.map_utils.editor

import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.data.type.*
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_utils.editor.type.*

typealias EditorFactory<T> = (args: SessionArgs, visualizer: Visualizer) -> DataEditor<T>

private fun <T> pair(data: Data<T>, factory: EditorFactory<T>): Pair<Data<T>, EditorFactory<T>> =
    Pair(data, factory)

private val DATA_EDITORS = mutableMapOf<Data<*>, EditorFactory<*>>(
    pair(BlockBoxData, ::BlockBoxEditor),
    pair(PositionData, ::PositionEditor),
    pair(CheckpointData, ::CheckpointEditor),
    pair(PositionedBlockSetData, ::PositionedBlockSetEditor),
    pair(SplinePathData, ::SplinePathEditor),
)

fun <T> createEditor(data: Data<T>, args: SessionArgs, visualizer: Visualizer): DataEditor<T> {
    val factory = DATA_EDITORS[data] ?: throw UnsupportedOperationException(
        "No editor is registered for data \"${data.id()}\""
    )

    @Suppress("UNCHECKED_CAST")
    return factory(args, visualizer) as DataEditor<T>
}