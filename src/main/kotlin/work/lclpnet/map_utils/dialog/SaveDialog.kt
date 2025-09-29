package work.lclpnet.map_utils.dialog

import net.minecraft.dialog.AfterAction
import net.minecraft.dialog.DialogActionButtonData
import net.minecraft.dialog.DialogButtonData
import net.minecraft.dialog.DialogCommonData
import net.minecraft.dialog.action.DynamicCustomDialogAction
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.input.TextInputControl
import net.minecraft.dialog.type.ConfirmationDialog
import net.minecraft.dialog.type.DialogInput
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.DataManager
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.identifier
import java.util.*

class SaveDialog(val translations: Translations, val dataManager: DataManager, val sessionManager: SessionManager) {

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInventoryHooks.SWAP_HANDS, PlayerInventoryHooks.SwapHands { player, _ ->
            if (sessionManager.isEditing(player)) {
                openSaveDialog(player)
                true
            } else {
                false
            }
        })
    }

    fun openSaveDialog(player: ServerPlayerEntity) {
        val session = sessionManager.optSession(player) ?: return
        val editor = session.editor ?: return
        val propertyId = editor.propertyId()

        val title = (if (propertyId == null) translations.translateText(
            "save.title_new",
            translations.translateText("type.${editor.data().id()}")
        ) else translations.translateText(
            "save.title",
            propertyId as Object
        )).translateFor(player)

        val body = mutableListOf<DialogBody>()

        editor.addBody(body, translations, player)

        val inputs = listOf(DialogInput(
            "propertyId",
            TextInputControl(
                200,
                translations.translateText("save.property_id").translateFor(player),
                true,
                propertyId ?: "",
                64,
                Optional.empty()
            )
        ))

        val dialog = ConfirmationDialog(
            DialogCommonData(
                title, Optional.empty(), true, true, AfterAction.CLOSE, body, inputs
            ),
            DialogActionButtonData(
                DialogButtonData(translations.translateText("save").translateFor(player), 150),
                Optional.of(DynamicCustomDialogAction(SAVE_ID, Optional.empty()))
            ),
            DialogActionButtonData(
                DialogButtonData(Text.translatable("gui.cancel"), 150),
                Optional.empty()
            ),
        )

        player.openDialog(RegistryEntry.of(dialog))
    }

    fun save(player: ServerPlayerEntity, payload: Optional<NbtElement>) {
        val session = sessionManager.optSession(player) ?: return
        val editor = session.editor ?: return

        val nbt = payload.map { it as? NbtCompound }.orElseGet { NbtCompound() }!!
        val propertyId = nbt.getString("propertyId", null) ?: return

        editor.saveToWorld(player.world, dataManager, propertyId, nbt)
    }

    companion object {
        val SAVE_ID = identifier("save")
    }
}