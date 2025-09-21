package work.lclpnet.map_utils.editor

import work.lclpnet.map_utils.data.DataEditor

class Session {

    var editor: DataEditor? = null
        private set

    fun setEditor(editor: DataEditor) {
        this.editor = editor
    }

    fun clearEditor() {
        this.editor = null
    }
}