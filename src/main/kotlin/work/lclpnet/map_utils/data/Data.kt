package work.lclpnet.map_utils.data

interface Data {
    fun id(): String
    fun createEditor(): DataEditor
}

interface DataEditor {
    fun data(): Data
}