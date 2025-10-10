package work.lclpnet.map_api.schema

import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.data.DataInstance

interface DataDefinition<D, T> {
    val name: String
    val data: Data<D>
    val role: String?
    val optional: Boolean
}

class SingleDataDefinition<T>(
    override val name: String,
    override val data: Data<T>,
    val default: T?,
    override val role: String?,
    override val optional: Boolean
) : DataDefinition<T, T> {

    fun makeDefaultInstance(): DataInstance<T>? {
        if (default == null) return null

        return DataInstance(data, default, role)
    }
}

class ListDataDefinition<T>(
    override val name: String,
    override val data: Data<T>,
    val default: List<T>?,
    override val role: String,
    override val optional: Boolean
) : DataDefinition<T, List<T>> {

    fun makeDefaultInstances(): List<DataInstance<T>>? {
        if (default == null) return null

        return default.map { DataInstance(data, it, role) }
    }
}