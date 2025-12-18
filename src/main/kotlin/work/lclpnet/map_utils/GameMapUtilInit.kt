package work.lclpnet.map_utils

import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.hook.ServerLifecycleHooks
import work.lclpnet.kibu.translate.util.ModTranslations
import work.lclpnet.map_api.hook.GameMapApiReadyCallback
import work.lclpnet.map_utils.dialog.*
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.schema.SchemaLoader
import work.lclpnet.map_utils.schema.SchemaManager
import work.lclpnet.map_utils.util.MapArchiver

const val MOD_ID = "game-map-utils"
val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

fun identifier(path: String): Identifier {
    return Identifier.fromNamespaceAndPath(MOD_ID, path)
}

fun init() {
    val translations = ModTranslations.fromAssets(MOD_ID, LOGGER, true).translations
    val hooks = HookContainer()

    GameMapApiReadyCallback.HOOK.register { api, _ ->
        val dataManager = api.dataManager

        val sessionManager = SessionManager(translations, dataManager)

        sessionManager.init(hooks)

        val saveDialog = SaveDialog(translations, dataManager, sessionManager)
        saveDialog.init(hooks)

        val schemaManager = SchemaManager(SchemaLoader(LOGGER), dataManager)
        schemaManager.init(hooks)

        val mapArchiver = MapArchiver(setOf(
            "advancements",
            "data/DistantHorizons.sqlite",
            "DIM1",
            "DIM-1",
            "playerdata",
            "stats",
            "icon.png",
            "level.dat_old",
            "session.lock",
        ))

        val mapManagerDialog = MapManagerDialog(translations, dataManager, mapArchiver, LOGGER)
        mapManagerDialog.init(hooks)

        DialogHandler(
            translations,
            CreateDialog(translations, sessionManager),
            saveDialog,
            ListDialog(translations, dataManager, sessionManager),
            SchemaSelectorDialog(schemaManager, sessionManager, translations),
            mapManagerDialog,
        ).init(hooks)
    }

    ServerLifecycleHooks.SERVER_STOPPED.register {
        hooks.unload()
    }

    LOGGER.info("Initialized")
}
