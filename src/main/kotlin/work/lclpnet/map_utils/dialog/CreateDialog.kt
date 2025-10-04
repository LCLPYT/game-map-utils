package work.lclpnet.map_utils.dialog

import net.minecraft.dialog.AfterAction
import net.minecraft.dialog.DialogActionButtonData
import net.minecraft.dialog.DialogButtonData
import net.minecraft.dialog.DialogCommonData
import net.minecraft.dialog.action.DynamicCustomDialogAction
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.type.MultiActionDialog
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting.RED
import net.minecraft.util.Formatting.YELLOW
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.DATA_TYPES
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.openConfirmDialog
import java.util.*

class CreateDialog(val translations: Translations, val sessionManager: SessionManager) {

    fun openOrConfirm(player: ServerPlayerEntity) {
        if (!player.isCreativeLevelTwoOp) {
            translations.translateText("missing_permission").formatted(RED).sendTo(player)
            return
        }

        if (sessionManager.optSession(player)?.editor != null) {
            val msg = translations.translateText("create.active_editor").formatted(YELLOW).translateFor(player)
            openConfirmDialog(player, translations, msg, CONFIRM_ID)
            return
        }

        open(player)
    }

    private fun open(player: ServerPlayerEntity) {
        val title = translations.translateText("create.title").translateFor(player)

        val body = listOf<DialogBody>()

        val buttons = DATA_TYPES.map { (id, _) ->
            val label = translations.translateText("type.$id").translateFor(player)

            val nbt = NbtCompound()
            nbt.putString("type", id)

            DialogActionButtonData(
                DialogButtonData(label, 150),
                Optional.of(DynamicCustomDialogAction(START_ID, Optional.of(nbt))))
        }

        val dialog = MultiActionDialog(
            DialogCommonData(
                title, Optional.empty(), true, true, AfterAction.CLOSE, body, listOf()
            ),
            buttons,
            Optional.of(DialogActionButtonData(
                DialogButtonData(Text.translatable("gui.cancel"), 150),
                Optional.empty()
            )),
            1
        )

        player.openDialog(RegistryEntry.of(dialog))
    }

    fun startEditing(player: ServerPlayerEntity, nbt: NbtCompound) {
        val typeId = nbt.getString("type", null)

        val type = DATA_TYPES[typeId] ?: return

        val session = sessionManager.getSession(player)
        session.setEditor(type.createEditor(session.args, session.playerVisualizer))
    }

    fun discardAndOpen(player: ServerPlayerEntity) {
        sessionManager.optSession(player)?.destroy()
        openOrConfirm(player)
    }

    companion object {
        val OPEN_ID = identifier("create_open")
        val START_ID = identifier("create_start")
        val CONFIRM_ID = identifier("create_confirm")
    }
}