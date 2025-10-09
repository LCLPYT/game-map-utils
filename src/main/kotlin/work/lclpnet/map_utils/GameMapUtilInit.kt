package work.lclpnet.map_utils

import net.minecraft.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.hook.ServerLifecycleHooks
import work.lclpnet.kibu.translate.util.ModTranslations
import work.lclpnet.map_api.GameMapApi
import work.lclpnet.map_utils.dialog.*
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.schema.SchemaLoader
import work.lclpnet.map_utils.schema.SchemaManager

const val MOD_ID = "game-map-utils"
val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

fun identifier(path: String): Identifier {
    return Identifier.of(MOD_ID, path)
}

fun init() {
    val translations = ModTranslations.fromAssets(MOD_ID, LOGGER, true).translations
    val hooks = HookContainer()

    ServerLifecycleHooks.SERVER_STARTED.register { server ->
        val api = GameMapApi.get(server)
        val dataManager = api.dataManager

        val sessionManager = SessionManager(translations, dataManager)

        sessionManager.init(hooks)

        val saveDialog = SaveDialog(translations, dataManager, sessionManager)
        saveDialog.init(hooks)

        val schemas = SchemaLoader(LOGGER).loadAll()
        val schemaManager = SchemaManager(schemas, dataManager)

        DialogHandler(
            translations,
            CreateDialog(translations, sessionManager),
            saveDialog,
            ListDialog(translations, dataManager, sessionManager),
            SchemaSelectorDialog(schemaManager, sessionManager, translations),
        ).init(hooks)
    }

    ServerLifecycleHooks.SERVER_STOPPED.register {
        hooks.unload()
    }

    LOGGER.info("Initialized")
}
