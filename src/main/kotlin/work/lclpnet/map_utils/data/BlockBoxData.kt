package work.lclpnet.map_utils.data

class BlockBoxData() : Data {
    override fun id() = "block_box"
    override fun createEditor() = BlockBoxEditor(this)
}

class BlockBoxEditor(val data: BlockBoxData) : DataEditor {
    override fun data() = data
}