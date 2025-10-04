package work.lclpnet.map_utils.editor

import net.minecraft.entity.boss.BossBar
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting.AQUA
import net.minecraft.util.Formatting.YELLOW
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.translate.bossbar.TranslatedBossBar
import work.lclpnet.map_utils.data.Data
import work.lclpnet.map_utils.data.DataManager
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.BossBarContainer
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.util.keybind
import work.lclpnet.map_utils.visual.PlayerSceneRenderer
import work.lclpnet.map_utils.visual.PlayerVisualizer

class Session(val args: SessionArgs, dynamicEntityManager: DynamicEntityManager, val dataManager: DataManager) {

    private val bossBars = BossBarContainer()
    private val hooks = HookContainer()
    private val editorHooks = HookContainer()
    private var editorBossBar: TranslatedBossBar? = null
    val playerVisualizer = PlayerVisualizer(args, dynamicEntityManager, PlayerSceneRenderer(args, dynamicEntityManager))
    val sessionVisualizer = PlayerVisualizer(args, dynamicEntityManager, PlayerSceneRenderer(args, dynamicEntityManager))
    val sessionRemovables = mutableMapOf<String, Removable>()
    var showAll = false

    var editor: DataEditor<*>? = null
        private set

    fun init() {
        bossBars.init(args.server().bossBarManager)
    }

    fun destroy() {
        destroyEditor()

        sessionRemovables.clear()
        sessionVisualizer.destroy()

        bossBars.destroy()
        hooks.unload()
    }

    fun destroyEditor() {
        editorHooks.unload()
        playerVisualizer.destroy()

        editorBossBar?.removePlayer(args.player())
        bossBars.destroy()

        this.editor = null
        editorBossBar = null

        if (showAll) {
            clearSessionRemovables()
            displayWorldData()
        }
    }

    fun player(): ServerPlayerEntity = args.player()

    fun setEditor(editor: DataEditor<*>) {
        this.editor = editor

        val barId = identifier("edit_${player().uuid.toString().replace("-", "").lowercase()}")

        val bar = (if (editor.propertyId == null || editor.isNew()) args.translations.translateBossBar(
            barId,
            "creating",
            args.translations.translateText("type.${editor.data().id()}"),
            keybind("swapOffhand").formatted(YELLOW)
        ) else args.translations.translateBossBar(
            barId,
            "editing",
            editor.propertyId,
            keybind("swapOffhand").formatted(YELLOW)
        )).with(bossBars).formatted(AQUA)

        bar.color = BossBar.Color.YELLOW
        bar.addPlayer(player())

        editor.init(editorHooks)
        editor.sendTutorial()

        editorBossBar = bar
    }

    @Synchronized
    fun addSessionRemovable(propertyId: String, removable: Removable) {
        sessionRemovables[propertyId] = removable
    }

    @Synchronized
    fun clearSessionRemovables() {
        for ((_, removable) in sessionRemovables) {
            removable.remove()
        }

        sessionRemovables.clear()
    }

    @Synchronized
    fun removeSessionDisplay(propertyId: String) {
        val removable = sessionRemovables.remove(propertyId) ?: return

        removable.remove()
    }

    @Synchronized
    fun displayWorldData() {
        val worldData = dataManager.getWorldData(args.world)

        for ((propertyId, dataInstance) in worldData.properties()) {
            val removable = dataInstance.display(sessionVisualizer, args.player(), args.translations, propertyId)
            addSessionRemovable(propertyId, removable)
        }
    }

    fun <T> createEditor(data: Data<T>): DataEditor<T> = data.createEditor(args, playerVisualizer)
}