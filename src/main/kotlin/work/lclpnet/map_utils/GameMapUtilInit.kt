package work.lclpnet.map_utils

import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.translate.util.ModTranslations
import work.lclpnet.map_utils.data.DataManager
import work.lclpnet.map_utils.dialog.CreateDialog
import work.lclpnet.map_utils.dialog.DialogHandler
import work.lclpnet.map_utils.dialog.SaveDialog
import work.lclpnet.map_utils.editor.SessionManager

const val MOD_ID = "game-map-utils"
val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

fun identifier(path: String): Identifier {
    return Identifier.of(MOD_ID, path)
}

fun init() {
    val translations = ModTranslations.fromAssets(MOD_ID, LOGGER, true).translations
    val dataManager = DataManager()
    val sessionManager = SessionManager(translations)
    val hooks = HookContainer()

    sessionManager.init(hooks)

    val saveDialog = SaveDialog(translations, sessionManager)
    saveDialog.init(hooks)

    DialogHandler(
        CreateDialog(translations, dataManager, sessionManager),
        saveDialog
    ).init(hooks)

    LOGGER.info("Initialized")
}
