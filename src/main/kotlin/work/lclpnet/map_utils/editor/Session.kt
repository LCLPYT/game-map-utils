package work.lclpnet.map_utils.editor

import net.minecraft.entity.boss.BossBar
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting.AQUA
import net.minecraft.util.Formatting.YELLOW
import work.lclpnet.gaco.scene.MixedMountContext
import work.lclpnet.gaco.scene.Scene
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.translate.bossbar.TranslatedBossBar
import work.lclpnet.map_utils.data.DataEditor
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.BossBarContainer
import work.lclpnet.map_utils.util.keybind

class Session(val args: SessionArgs) {

    private val bossBars = BossBarContainer()
    private val hooks = HookContainer()
    private var bossBar: TranslatedBossBar? = null
    private val scene: Scene = Scene(MixedMountContext(args.world, args.dynamicEntityManager))

    var editor: DataEditor<*>? = null
        private set

    fun init() {
        bossBars.init(args.server().bossBarManager)
    }

    fun destroy() {
        editor = null
        bossBar = null
        bossBars.destroy()
        hooks.unload()
        scene.clear()
    }

    fun player(): ServerPlayerEntity = args.player()

    fun setEditor(editor: DataEditor<*>) {
        this.editor = editor

        val barId = identifier("edit_${player().uuid.toString().replace("-", "").lowercase()}")

        val bar = (if (editor.propertyId() == null) args.translations.translateBossBar(
            barId,
            "creating",
            args.translations.translateText("type.${editor.data().id()}"),
            keybind("swapOffhand").formatted(YELLOW)
        ) else args.translations.translateBossBar(
            barId,
            "editing",
            editor.propertyId(),
            keybind("swapOffhand").formatted(YELLOW)
        )).with(bossBars).formatted(AQUA)

        bar.color = BossBar.Color.YELLOW
        bar.addPlayer(player())

        editor.init(hooks, args)

        bossBar = bar
    }
}