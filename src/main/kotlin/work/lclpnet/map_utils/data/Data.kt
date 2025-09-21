package work.lclpnet.map_utils.data

interface Data {
    fun id(): String
    fun createEditor(propertyId: String?): DataEditor
}

interface DataEditor {
    fun data(): Data
    fun propertyId(): String?
}