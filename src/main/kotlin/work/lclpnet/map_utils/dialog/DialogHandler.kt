package work.lclpnet.map_utils.dialog

import net.minecraft.nbt.NbtElement
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.network.CustomClickActionCallback
import java.util.*

class DialogHandler(val createDialog: CreateDialog, val saveDialog: SaveDialog) {

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(CustomClickActionCallback.HOOK, CustomClickActionCallback { player, id, payload ->
            onCustomClick(player, id, payload)
        })
    }

    fun onCustomClick(player: ServerPlayerEntity, id: Identifier, payload: Optional<NbtElement>) {
        when (id) {
            CreateDialog.OPEN_ID -> createDialog.openOrConfirm(player)
            CreateDialog.START_ID -> createDialog.startEditing(player, payload)
            CreateDialog.CONFIRM_ID -> createDialog.discardAndOpen(player)
            SaveDialog.SAVE_ID -> saveDialog.save(player, payload)
            SaveDialog.CONFIRM_ID -> saveDialog.saveDataToWorld(player)
        }
    }
}