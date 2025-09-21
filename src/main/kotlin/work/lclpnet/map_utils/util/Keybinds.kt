package work.lclpnet.map_utils.util

import net.minecraft.text.MutableText
import net.minecraft.text.Text

fun keybind(key: String, vararg extra: String): MutableText {
    val text = Text.keybind("key.$key")

    for (k in extra) {
        text.append(" + ").append(Text.keybind("key.$k"))
    }

    return Text.literal("[").append(text).append("]")
}
