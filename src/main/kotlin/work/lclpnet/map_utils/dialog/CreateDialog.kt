package work.lclpnet.map_utils.dialog

import net.minecraft.dialog.AfterAction
import net.minecraft.dialog.DialogActionButtonData
import net.minecraft.dialog.DialogButtonData
import net.minecraft.dialog.DialogCommonData
import net.minecraft.dialog.action.DynamicCustomDialogAction
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.input.SingleOptionInputControl
import net.minecraft.dialog.type.ConfirmationDialog
import net.minecraft.dialog.type.DialogInput
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

        val inputs = listOf(typeInput(player))

        val dialog = ConfirmationDialog(
            DialogCommonData(
                title, Optional.empty(), true, true, AfterAction.CLOSE, body, inputs
            ),
            DialogActionButtonData(
                DialogButtonData(translations.translateText("create").translateFor(player), 150),
                Optional.of(DynamicCustomDialogAction(START_ID, Optional.empty()))
            ),
            DialogActionButtonData(
                DialogButtonData(Text.translatable("gui.cancel"), 150),
                Optional.empty()
            ),
        )

        player.openDialog(RegistryEntry.of(dialog))
    }

    private fun typeInput(player: ServerPlayerEntity): DialogInput {
        val types = DATA_TYPES.map { (id, _) ->
            val label = translations.translateText("type.$id").translateFor(player)

            SingleOptionInputControl.Entry(id, Optional.of(label), false)
        }

        return DialogInput(
            "type",
            SingleOptionInputControl(
                200,
                types,
                translations.translateText("create.type").translateFor(player),
                true
            )
        )
    }

    fun startEditing(player: ServerPlayerEntity, nbt: NbtCompound) {
        val typeId = nbt.getString("type", null)

        val type = DATA_TYPES[typeId] ?: return

        val session = sessionManager.getSession(player)
        session.setEditor(type.createEditor(session.args, session.editorVisualizer))
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