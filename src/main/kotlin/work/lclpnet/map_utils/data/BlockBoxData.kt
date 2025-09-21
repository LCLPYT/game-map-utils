package work.lclpnet.map_utils.data

class BlockBoxData() : Data {
    override fun id() = "block_box"
    override fun createEditor(propertyId: String?) = BlockBoxEditor(this, propertyId)
}

class BlockBoxEditor(val data: BlockBoxData, val propertyId: String?) : DataEditor {
    override fun data() = data
    override fun propertyId() = propertyId
}