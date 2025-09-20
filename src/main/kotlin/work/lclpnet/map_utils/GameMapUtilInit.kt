package work.lclpnet.map_utils

import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val MOD_ID = "game-map-utils"
val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

fun identifier(path: String): Identifier {
    return Identifier.of(MOD_ID, path)
}

fun init() {
    LOGGER.info("Initialized")
}
