package work.lclpnet.map_utils.dialog

import net.minecraft.nbt.NbtCompound
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
        val nbt = payload.map { it as? NbtCompound }.orElseGet { NbtCompound() }!!

        when (id) {
            CreateDialog.OPEN_ID -> createDialog.openOrConfirm(player)
            CreateDialog.START_ID -> createDialog.startEditing(player, nbt)
            CreateDialog.CONFIRM_ID -> createDialog.discardAndOpen(player)
            SaveDialog.SAVE_ID -> saveDialog.save(player, nbt)
            SaveDialog.CONFIRM_ID -> saveDialog.saveDataToWorld(player, nbt)
            SaveDialog.DISCARD_ID -> saveDialog.discard(player, nbt)
            SaveDialog.CLOSE_ID -> saveDialog.onClose(player, nbt)
            ListDialog.LIST_ID -> listDialog.open(player)
            ListDialog.SELECT_ID -> listDialog.select(player, nbt)
            ListDialog.CONFIRM_SELECT_ID -> listDialog.confirmSelect(player, nbt)
            ListDialog.DELETE_ID -> listDialog.delete(player, nbt)
            ListDialog.CONFIRM_DELETE_ID -> listDialog.confirmDelete(player, nbt)
            ListDialog.MOVE_UP_ID -> listDialog.moveUp(player, nbt)
            ListDialog.MOVE_DOWN_ID -> listDialog.moveDown(player, nbt)
        }
    }
}