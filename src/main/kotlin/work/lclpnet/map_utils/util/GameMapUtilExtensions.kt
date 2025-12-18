package work.lclpnet.map_utils.util

import net.minecraft.world.phys.Vec3
import work.lclpnet.kibu.translate.text.LocalizedFormat

fun Vec3.toLocalizedShortString(): LocalizedFormat = LocalizedFormat.format("%.2f, %.2f, %.2f", x, y, z)
