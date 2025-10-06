package work.lclpnet.map_api.schema

import work.lclpnet.map_api.data.Data

interface DataDefinition<D, T> {
    val name: String
    val data: Data<D>
    val default: T?
    val role: String?
    val optional: Boolean
}

class SingleDataDefinition<T>(
    override val name: String,
    override val data: Data<T>,
    override val default: T?,
    override val role: String?,
    override val optional: Boolean
) : DataDefinition<T, T>

class ListDataDefinition<T>(
    override val name: String,
    override val data: Data<T>,
    override val default: List<T>?,
    override val role: String?,
    override val optional: Boolean
) : DataDefinition<T, List<T>>