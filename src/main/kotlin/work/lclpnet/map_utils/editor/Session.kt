package work.lclpnet.map_utils.editor

import net.minecraft.entity.boss.BossBar
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting.AQUA
import net.minecraft.util.Formatting.YELLOW
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

        val barId = identifier("edit_${player().uuid.toString().replace("-", "").lowercase()}")

        val bar = (if (editor.propertyId() == null) args.translations.translateBossBar(
            barId,
            "creating",
            args.translations.translateText("type.${editor.data().id()}"),
            Text.keybind("key.swapOffhand").formatted(YELLOW)
        ) else args.translations.translateBossBar(
            barId,
            "editing",
            editor.propertyId(),
            Text.keybind("key.swapOffhand").formatted(YELLOW)
        )).with(bossBars).formatted(AQUA)

        bar.color = BossBar.Color.YELLOW
        bar.addPlayer(player())

        bossBar = bar
    }
}