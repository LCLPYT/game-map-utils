package work.lclpnet.map_api.util

import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun getRandomHsvColor(random: Random): Int {
    // hue between 0 and 360
    val hue = random.nextFloat() * 360

    return getRandomHsvColor(random, hue)
}

fun getRandomHsvColor(random: Random, hue: Float): Int {
    // saturation between 0.6 and 1
    val saturation = random.nextFloat() * 0.4f + 0.6f
    // value between 0.9 and 1
    val value = random.nextFloat() * 0.1f + 0.9f

    return hsvToRgb(hue, saturation, value)
}

/**
 * Convert an HSV color to a packed ARGB int.
 * Source: [Wikipedia](https://en.wikipedia.org/w/index.php?title=HSL_and_HSV&oldid=1147621409#HSV_to_RGB_alternative)
 * @param hue Hue [0, 360]
 * @param saturation Saturation [0, 1]
 * @param value Value [0, 1]
 * @return A packed argb integer with 8 bits each (alpha, red, green, blue).
 */
fun hsvToRgb(hue: Float, saturation: Float, value: Float): Int {
    val hueDiv = hue / 60

    var k: Float = (5 + hueDiv) % 6
    val r = value - value * saturation * max(0f, min(min(k, 4 - k), 1f))

    k = (3 + hueDiv) % 6
    val g = value - value * saturation * max(0f, min(min(k, 4 - k), 1f))

    k = (1 + hueDiv) % 6
    val b = value - value * saturation * max(0f, min(min(k, 4 - k), 1f))

    return getRgbPacked(
        max(0, min(255, (255 * r).roundToInt())),
        max(0, min(255, (255 * g).roundToInt())),
        max(0, min(255, (255 * b).roundToInt()))
    )
}

fun getRgbPacked(red: Int, green: Int, blue: Int): Int = red shl 16 or (green shl 8) or blue
