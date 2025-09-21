package work.lclpnet.map_utils.data

class BlockBoxData() : Data {
    override fun id() = "block_box"
    override fun createEditor() = BlockBoxEditor()
}

class BlockBoxEditor : DataEditor {

}