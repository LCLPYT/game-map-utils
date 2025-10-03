package work.lclpnet.map_utils.util

import com.google.gson.FormattingStyle
import com.google.gson.JsonElement
import com.google.gson.Strictness
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonWriter
import java.io.StringWriter
import java.util.function.Function
import java.util.regex.MatchResult
import java.util.regex.Pattern

fun JsonElement.toPrettyString(): String {
    val stringWriter = StringWriter()
    val jsonWriter = JsonWriter(stringWriter)

    jsonWriter.formattingStyle = FormattingStyle.PRETTY
    jsonWriter.strictness = Strictness.STRICT
    Streams.write(this, jsonWriter)

    return prettifyJson(stringWriter.toString())
}

fun prettifyJson(content: String): String {
    var pretty = content

    // prettify BlockPos tuples like [[x1, y1, z1], [x2, y2, z2]]
    pretty = prettifyPosTuple(
        "\\[\\s*(?:\\[(?:\\s*-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?,?){3}\\s*],?\\s*){2}]",
        pretty
    )

    // prettify BlockPos tuples like [x, y, z]
    pretty = prettifyPosTuple("\\[(?:\\s*-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?,?){3}\\s*]", pretty)

    return pretty
}

private fun prettifyPosTuple(regex: String, content: String): String {
    val tuplePairPattern = Pattern.compile(regex)

    return tuplePairPattern.matcher(content).replaceAll(Function { matchResult: MatchResult? ->
        val match = matchResult!!.group()
        match.replace("\\s+".toRegex(), "").replace(",".toRegex(), ", ")
    })
}
