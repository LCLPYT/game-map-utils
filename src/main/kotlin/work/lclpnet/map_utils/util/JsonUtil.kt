package work.lclpnet.map_utils.util

import com.google.gson.FormattingStyle
import com.google.gson.JsonElement
import com.google.gson.Strictness
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonWriter
import java.io.StringWriter

fun JsonElement.toPrettyString(): String {
    val stringWriter = StringWriter()
    val jsonWriter = JsonWriter(stringWriter)

    jsonWriter.formattingStyle = FormattingStyle.PRETTY
    jsonWriter.strictness = Strictness.STRICT
    Streams.write(this, jsonWriter)

    return stringWriter.toString()
}