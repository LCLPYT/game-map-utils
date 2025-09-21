package work.lclpnet.map_utils

import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.translate.util.ModTranslations
import work.lclpnet.map_utils.data.DataManager
import work.lclpnet.map_utils.dialog.DialogHandler

const val MOD_ID = "game-map-utils"
val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

fun identifier(path: String): Identifier {
    return Identifier.of(MOD_ID, path)
}

fun init() {
    val translations = ModTranslations.fromAssets(MOD_ID, LOGGER, true).translations
    val dataManager = DataManager()

    DialogHandler(translations, dataManager).init(HookContainer())

    LOGGER.info("Initialized")
}
