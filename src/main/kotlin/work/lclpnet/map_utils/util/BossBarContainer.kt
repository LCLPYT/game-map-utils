package work.lclpnet.map_utils.util

import net.minecraft.entity.boss.BossBarManager
import net.minecraft.entity.boss.CommandBossBar
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import work.lclpnet.kibu.translate.bossbar.BossBarProvider
import work.lclpnet.kibu.translate.util.TransientBossBars

class BossBarContainer() : BossBarProvider {

    private val bars: MutableSet<CommandBossBar> = HashSet()
    private var bossBarManager: BossBarManager? = null

    @Synchronized
    fun init(bossBarManager: BossBarManager) {
        if (this.bossBarManager != null) return

        this.bossBarManager = bossBarManager
    }

    @Synchronized
    override fun createBossBar(id: Identifier, text: Text): CommandBossBar {
        val bossBarManager = bossBarManager ?: throw IllegalStateException("Boss bar container not initialized")

        val bar = bossBarManager.add(id, text)

        TransientBossBars.setTransient(bar, true)

        bars.add(bar)

        return bar
    }

    @Synchronized
    fun removeBossBar(bossBar: CommandBossBar) {
        removeBossBarInternal(bossBar)
        bars.remove(bossBar)
    }

    @Synchronized
    fun destroy() {
        bars.forEach { removeBossBarInternal(it) }
        bars.clear()
    }

    private fun removeBossBarInternal(bossBar: CommandBossBar) {
        bossBar.clearPlayers()
        bossBarManager?.remove(bossBar)
    }
}