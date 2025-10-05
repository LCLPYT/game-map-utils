package work.lclpnet.map_utils.editor

import work.lclpnet.map_api.data.Data

abstract class BaseDataEditor<T>(val data: Data<T>) : DataEditor<T> {

    override fun data() = data
    override fun isNew() = prevPropertyId == null
}