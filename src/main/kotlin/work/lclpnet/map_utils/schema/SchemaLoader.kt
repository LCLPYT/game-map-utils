package work.lclpnet.map_utils.schema

import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import net.fabricmc.loader.api.FabricLoader
import org.json.JSONObject
import org.slf4j.Logger
import work.lclpnet.map_api.data.DATA_TYPES
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.schema.DataDefinition
import work.lclpnet.map_api.schema.ListDataDefinition
import work.lclpnet.map_api.schema.MapSchema
import work.lclpnet.map_api.schema.SingleDataDefinition
import work.lclpnet.map_api.util.json2gson
import work.lclpnet.map_utils.MOD_ID
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

private fun interface DataDefinitionFactory {
    fun create(propertyId: String, name: String, role: String?, optional: Boolean): DataDefinition<*, *>
}

val SCHEMA_DIR: Path = FabricLoader.getInstance().configDir
    .resolve(MOD_ID)
    .resolve("map_schemas")

class SchemaLoader(val logger: Logger) {

    fun loadAll(): Map<String, MapSchema> {

        if (!SCHEMA_DIR.isDirectory()) {
            SCHEMA_DIR.createDirectories()
            return emptyMap()
        }

        val matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.json")
        val schemas = mutableListOf<MapSchema>()

        Files.walk(SCHEMA_DIR).use { paths ->
            paths.filter { it.isRegularFile() && matcher.matches(it) }
                .forEach {
                   val schema = load(SCHEMA_DIR, it)

                    if (schema != null) {
                        schemas.add(schema)
                    }
                }
        }

        return schemas.associateBy { it.id }.toMap()
    }

    fun load(rootDir: Path, path: Path): MapSchema? {
        if (!path.isRegularFile()) return null

        val text = path.readText(StandardCharsets.UTF_8)
        val json = JSONObject(text)

        val name = json.getString("name")
        val properties = readProperties(json.getJSONObject("properties"))

        val id = rootDir.relativize(path).toString()

        return MapSchema(id, name, properties)
    }

    private fun readProperties(json: JSONObject): Map<String, DataDefinition<*, *>> {
        val properties = mutableMapOf<String, DataDefinition<*, *>>()

        for (propertyId in json.keySet()) {
            val property = readProperty(json.getJSONObject(propertyId), propertyId) ?: continue

            properties[propertyId] = property
        }

        return properties
    }

    private fun readProperty(json: JSONObject, propertyId: String): DataDefinition<*, *>? {
        val name = json.optString("name", propertyId)
        val role = json.optString("role", null)
        val optional = json.optBoolean("optional", false)
        val input = json2gson(json.opt("default"))

        val type = json.getString("type")

        if (type == "list") {
            val itemType = json.getString("items")

            val itemData = DATA_TYPES[itemType] ?: return null

            return parseListData(itemData, input).create(propertyId, name, role, optional)
        }

        val data = DATA_TYPES[type] ?: return null

        return parseSingleData(data, input).create(propertyId, name, role, optional)
    }

    private fun <T> parseSingleData(data: Data<T>, json: JsonElement?): DataDefinitionFactory {
        val default = if (json == null) null else data.codec().decode(JsonOps.INSTANCE, json)
            .resultOrPartial { logger.error("Failed to parse default value: $it") }
            .map { it.first }
            .orElse(null)

        return DataDefinitionFactory { _, name, role, optional ->
            SingleDataDefinition(name, data, default, role, optional)
        }
    }

    private fun <T> parseListData(data: Data<T>, json: JsonElement?): DataDefinitionFactory {
        val default = if (json == null) null else data.codec().listOf().decode(JsonOps.INSTANCE, json)
            .resultOrPartial { logger.error("Failed to parse list default items: $it") }
            .map { it.first }
            .orElse(null)

        // ListDataDefinition collects all map properties with the defined role.
        // if no role is defined, the propertyId is used as role instead

        return DataDefinitionFactory { propertyId, name, role, optional ->
            ListDataDefinition(name, data, default, role ?: propertyId, optional)
        }
    }
}