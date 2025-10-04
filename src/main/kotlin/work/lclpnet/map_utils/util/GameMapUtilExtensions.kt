package work.lclpnet.map_utils.util

import net.minecraft.util.math.Vec3d
import work.lclpnet.kibu.translate.text.LocalizedFormat

fun Vec3d.toLocalizedShortString(): LocalizedFormat = LocalizedFormat.format("%.2f, %.2f, %.2f", x, y, z)
