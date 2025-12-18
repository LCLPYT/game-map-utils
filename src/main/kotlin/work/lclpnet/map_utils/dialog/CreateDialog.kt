package work.lclpnet.map_utils.dialog

import net.minecraft.ChatFormatting.YELLOW
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.*
import net.minecraft.server.dialog.action.CustomAll
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.level.ServerPlayer
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.openConfirmDialog
import java.util.*

class CreateDialog(val translations: Translations, val sessionManager: SessionManager) {

    fun openOrConfirm(player: ServerPlayer) {
        if (sessionManager.optSession(player)?.editor != null) {
            val msg = translations.translateText("create.active_editor").formatted(YELLOW).translateFor(player)
            openConfirmDialog(player, translations, msg, CONFIRM_ID)
            return
        }

        open(player)
    }

    private fun open(player: ServerPlayer) {
        val title = translations.translateText("create.title").translateFor(player)

        val body = listOf<DialogBody>()

        val buttons = DataManager.DATA_TYPES.map { (id, _) ->
            val label = translations.translateText("type.$id").translateFor(player)

            val nbt = CompoundTag()
            nbt.putString("type", id)

            ActionButton(
                CommonButtonData(label, 150),
                Optional.of(CustomAll(START_ID, Optional.of(nbt))))
        }

        val dialog = MultiActionDialog(
            CommonDialogData(
                title, Optional.empty(), true, true, DialogAction.CLOSE, body, listOf()
            ),
            buttons,
            Optional.of(
                ActionButton(
                CommonButtonData(Component.translatable("gui.cancel"), 150),
                Optional.empty()
            )),
            1
        )

        player.openDialog(Holder.direct(dialog))
    }

    fun startEditing(player: ServerPlayer, nbt: CompoundTag) {
        val typeId = nbt.getString("type").orElse(null)

        val data = DataManager.DATA_TYPES[typeId] ?: return

        val session = sessionManager.getSession(player)
        session.setEditor(session.createEditor(data))
    }

    fun discardAndOpen(player: ServerPlayer) {
        sessionManager.optSession(player)?.destroy()
        openOrConfirm(player)
    }

    companion object {
        val OPEN_ID = identifier("create_open")
        val START_ID = identifier("create_start")
        val CONFIRM_ID = identifier("create_confirm")
    }
}