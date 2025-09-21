package work.lclpnet.map_utils.data

class DataManager {
    val types = listOf<Data>(
        BlockBoxData()
    ).associateBy { it.id() }
}