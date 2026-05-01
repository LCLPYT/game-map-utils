package work.lclpnet.map_utils.util

import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.bossevents.CustomBossEvent
import net.minecraft.server.bossevents.CustomBossEvents
import net.minecraft.util.RandomSource
import work.lclpnet.kibu.translate.bossbar.BossBarProvider
import work.lclpnet.kibu.translate.util.TransientBossBars

class BossBarContainer : BossBarProvider {

    private val bars: MutableSet<CustomBossEvent> = HashSet()
    private val random = RandomSource.createThreadLocalInstance()
    private var bossBarManager: CustomBossEvents? = null

    @Synchronized
    fun init(bossBarManager: CustomBossEvents) {
        if (this.bossBarManager != null) return

        this.bossBarManager = bossBarManager
    }

    @Synchronized
    override fun createBossBar(id: Identifier, text: Component): CustomBossEvent {
        val bossBarManager = bossBarManager ?: throw IllegalStateException("Boss bar container not initialized")

        val bar = bossBarManager.create(random, id, text)

        TransientBossBars.setTransient(bar, true)

        bars.add(bar)

        return bar
    }

    @Synchronized
    fun removeBossBar(bossBar: CustomBossEvent) {
        removeBossBarInternal(bossBar)
        bars.remove(bossBar)
    }

    @Synchronized
    fun destroy() {
        bars.forEach { removeBossBarInternal(it) }
        bars.clear()
    }

    private fun removeBossBarInternal(bossBar: CustomBossEvent) {
        bossBar.removeAllPlayers()
        bossBarManager?.remove(bossBar)
    }
}