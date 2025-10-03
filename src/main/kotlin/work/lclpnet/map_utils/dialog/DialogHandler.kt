package work.lclpnet.map_utils.dialog

import net.minecraft.nbt.NbtElement
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.network.CustomClickActionCallback
import java.util.*

class DialogHandler(val createDialog: CreateDialog, val saveDialog: SaveDialog, val listDialog: ListDialog) {

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
            SaveDialog.DISCARD_ID -> saveDialog.discard(player)
            SaveDialog.CLOSE_ID -> saveDialog.onClose(player, payload)
            ListDialog.LIST_ID -> listDialog.open(player)
            ListDialog.SELECT_ID -> listDialog.select(player, payload)
            ListDialog.CONFIRM_SELECT_ID -> listDialog.confirmSelect(player, payload)
        }
    }
}