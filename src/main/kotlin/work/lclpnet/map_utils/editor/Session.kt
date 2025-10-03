package work.lclpnet.map_utils.editor

import net.minecraft.entity.boss.BossBar
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting.AQUA
import net.minecraft.util.Formatting.YELLOW
import work.lclpnet.gaco.dynamic_entities.DynamicEntityManager
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.translate.bossbar.TranslatedBossBar
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.BossBarContainer
import work.lclpnet.map_utils.util.keybind

class Session(val args: SessionArgs, dynamicEntityManager: DynamicEntityManager) {

    private val bossBars = BossBarContainer()
    private val hooks = HookContainer()
    private val editorHooks = HookContainer()
    private var editorBossBar: TranslatedBossBar? = null
    val editorVisualizer = EditorVisualizer(args, dynamicEntityManager)

    var editor: DataEditor<*>? = null
        private set

    fun init() {
        bossBars.init(args.server().bossBarManager)
    }

    fun destroy() {
        destroyEditor()
        bossBars.destroy()
        hooks.unload()
    }

    fun destroyEditor() {
        editorHooks.unload()
        editorVisualizer.destroy()

        editorBossBar?.removePlayer(args.player())
        bossBars.destroy()

        this.editor = null
        editorBossBar = null
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

        editorBossBar = bar
    }
}