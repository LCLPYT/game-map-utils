package work.lclpnet.map_utils.editor

import net.minecraft.ChatFormatting.AQUA
import net.minecraft.ChatFormatting.YELLOW
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.BossEvent
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.translate.bossbar.TranslatedBossBar
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.data.DataInstance
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.BossBarContainer
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
    val shown = mutableSetOf<String>()

    var editor: DataEditor<*>? = null
        private set

    fun init() {
        bossBars.init(args.server().customBossEvents)

        playerVisualizer.init(hooks)
        sessionVisualizer.init(hooks)
    }

    fun destroy() {
        destroyEditor()

        sessionRemovables.clear()
        sessionVisualizer.destroy()

        bossBars.destroy()
        hooks.unload()
    }

    fun deactivateEditor() {
        editorHooks.unload()

        editorBossBar?.removePlayer(args.player())
        bossBars.destroy()
        editorBossBar = null
    }

    fun reactivateEditor() {
        val editor = this.editor ?: return

        val barId = identifier("edit_${player().uuid.toString().replace("-", "").lowercase()}")

        val bar = (if (editor.propertyId == null || editor.isNew()) args.translations.translateBossBar(
            barId,
            "creating",
            args.translations.translateText("type.${editor.data().id()}"),
            keybind("swapOffhand").withStyle(YELLOW)
        ) else args.translations.translateBossBar(
            barId,
            "editing",
            editor.propertyId,
            keybind("swapOffhand").withStyle(YELLOW)
        )).with(bossBars).formatted(AQUA)

        bar.color = BossEvent.BossBarColor.YELLOW
        bar.addPlayer(player())

        editor.init(editorHooks)
        editor.sendTutorial()

        editorBossBar = bar
    }

    fun destroyEditor() {
        deactivateEditor()

        playerVisualizer.destroy()

        this.editor = null

        updateShownDisplays()
    }

    fun player(): ServerPlayer = args.player()

    fun setEditor(editor: DataEditor<*>) {
        this.editor = editor

        reactivateEditor()
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
    fun updateShownDisplays() {
        clearSessionRemovables()

        val worldData = dataManager.getWorldData(args.world)
        val properties = worldData.properties()

        for (propertyId in shown) {
            if (editor?.propertyId == propertyId) continue

            val dataInstance = properties[propertyId] ?: continue

            val removable = dataInstance.display(sessionVisualizer, args.player(), args.translations, propertyId)
            addSessionRemovable(propertyId, removable)
        }
    }

    fun <T> createEditor(data: Data<T>): DataEditor<T> = createEditor(data, args, playerVisualizer)

    fun <T> createEditorFrom(dataInstance: DataInstance<T>, propertyId: String): DataEditor<T> {
        val editor = createEditor(dataInstance.data)
        editor.propertyId = propertyId
        editor.prevPropertyId = propertyId
        editor.role = dataInstance.role

        editor.load(dataInstance.value)

        return editor
    }
}