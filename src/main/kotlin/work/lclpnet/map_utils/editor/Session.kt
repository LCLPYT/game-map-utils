package work.lclpnet.map_utils.editor

import net.minecraft.entity.boss.BossBar
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting.AQUA
import work.lclpnet.kibu.translate.bossbar.TranslatedBossBar
import work.lclpnet.map_utils.data.DataEditor
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.BossBarContainer

class Session(val args: SessionArgs) {

    private val bossBars = BossBarContainer()
    private var bossBar: TranslatedBossBar? = null

    var editor: DataEditor? = null
        private set

    fun init() {
        bossBars.init(args.server().bossBarManager)
    }

    fun destroy() {
        editor = null
        bossBar = null
        bossBars.destroy()
    }

    fun player(): ServerPlayerEntity = args.player()

    fun setEditor(editor: DataEditor) {
        this.editor = editor

        val bar = args.translations.translateBossBar(
            identifier("edit_${player().uuid.toString().replace("-", "").lowercase()}"),
            "editing",
            args.translations.translateText("type.${editor.data().id()}")
        ).with(bossBars).formatted(AQUA)

        bar.color = BossBar.Color.YELLOW
        bar.addPlayer(player())

        bossBar = bar
    }
}