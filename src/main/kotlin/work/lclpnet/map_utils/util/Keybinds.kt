package work.lclpnet.map_utils.util

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

fun keybind(key: String, vararg extra: String): MutableComponent {
    val text = Component.keybind("key.$key")

    for (k in extra) {
        text.append(" + ").append(Component.keybind("key.$k"))
    }

    return Component.literal("[").append(text).append("]")
}
