package work.lclpnet.map_utils.editor

import work.lclpnet.map_utils.data.Data
import work.lclpnet.map_utils.data.DataEditor

abstract class BaseDataEditor<T>(val data: Data<T>) : DataEditor<T> {

    val new = propertyId == null

    override fun data() = data
    override fun isNew() = new


}