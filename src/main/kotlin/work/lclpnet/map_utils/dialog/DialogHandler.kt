package work.lclpnet.map_utils.dialog

import net.minecraft.nbt.NbtElement
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.network.CustomClickActionCallback
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.DataManager
import java.util.*

class DialogHandler(val translations: Translations, val dataManager: DataManager) {

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(CustomClickActionCallback.HOOK, CustomClickActionCallback { player, id, payload ->
            onCustomClick(player, id, payload)
        })
    }

    fun onCustomClick(player: ServerPlayerEntity, id: Identifier, payload: Optional<NbtElement>) {
        if (id == CreateDialog.ID) {
            CreateDialog(translations, dataManager).open(player)
        }
    }
}