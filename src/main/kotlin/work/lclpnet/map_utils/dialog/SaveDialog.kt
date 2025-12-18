package work.lclpnet.map_utils.dialog

import net.minecraft.ChatFormatting.*
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.*
import net.minecraft.server.dialog.action.CustomAll
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.dialog.input.TextInput
import net.minecraft.server.level.ServerPlayer
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.openConfirmDialog
import java.util.*

class SaveDialog(val translations: Translations, val dataManager: DataManager, val sessionManager: SessionManager) {

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInventoryHooks.SWAP_HANDS, PlayerInventoryHooks.SwapHands { player, _ ->
            if (sessionManager.isEditing(player) && !player.isShiftKeyDown && !player.lastClientInput.sprint) {
                openSaveDialog(player)
                true
            } else {
                false
            }
        })
    }

    fun openSaveDialog(player: ServerPlayer) {
        val session = sessionManager.optSession(player) ?: return
        val editor = session.editor ?: return
        val propertyId = editor.propertyId

        val title = (if (propertyId == null || editor.isNew()) translations.translateText(
            "save.title_new",
            translations.translateText("type.${editor.data().id()}")
        ) else translations.translateText(
            "save.title",
            propertyId as Object
        )).translateFor(player)

        val body = mutableListOf<DialogBody>()

        val inputs = mutableListOf(
            Input(
                "propertyId",
                TextInput(
                    200,
                    translations.translateText("save.property_id").translateFor(player),
                    true,
                    propertyId ?: "",
                    64,
                    Optional.empty()
                )
            ),
        )

        editor.modifyDialog(body, inputs, translations, player)

        val dialog = MultiActionDialog(
            CommonDialogData(
                title, Optional.empty(), true, true, DialogAction.CLOSE, body, inputs
            ),
            listOf(
                ActionButton(
                    CommonButtonData(translations.translateText("save").translateFor(player), 150),
                    Optional.of(CustomAll(SAVE_ID, Optional.empty()))
                ),
                ActionButton(
                    CommonButtonData(translations.translateText(if (editor.isNew()) "discard" else "discard_changes")
                        .formatted(RED)
                        .translateFor(player), 150),
                    Optional.of(CustomAll(DISCARD_ID, Optional.empty()))
                )
            ),
            Optional.of(
                ActionButton(
                CommonButtonData(Component.translatable("gui.cancel"), 150),
                Optional.of(CustomAll(CLOSE_ID, Optional.empty()))
            )),
            1
        )

        player.openDialog(Holder.direct(dialog))
    }

    fun save(player: ServerPlayer, nbt: CompoundTag) {
        val session = sessionManager.optSession(player) ?: return
        val editor = session.editor ?: return

        val propertyId = nbt.getStringOr("propertyId", "")

        editor.propertyId = propertyId
        editor.onDataChanged(nbt)

        if (propertyId.isBlank()) {
            translations.translateText(
                "save.missing",
                translations.translateText("save.property_id").formatted(YELLOW)
            ).formatted(RED).sendTo(player)
            return
        }

        if (editor.create(nbt) == null) return

        if (dataManager.hasData(player.level(), propertyId) && editor.prevPropertyId != propertyId) {
            val msg = translations.translateText(
                "save.overwrite",
                styled(propertyId, YELLOW)
            ).formatted(RED).translateFor(player)

            openConfirmDialog(player, translations, msg, CONFIRM_ID, payload = Optional.of(nbt))
            return
        }

        saveDataToWorld(player, nbt)
    }

    fun saveDataToWorld(player: ServerPlayer, nbt: CompoundTag) {
        val session = sessionManager.optSession(player) ?: return
        val editor = session.editor ?: return
        val propertyId = editor.propertyId ?: return

        if (!editor.saveToWorld(player.level(), dataManager, propertyId, editor.role, nbt)) return

        editor.onTerminate(nbt)

        val oldPropertyId = editor.prevPropertyId

        if (oldPropertyId != null && oldPropertyId != propertyId) {
            // renamed
            dataManager.removeData(player.level(), oldPropertyId)
        }

        translations.translateText(
            "save.saved",
            styled(propertyId, YELLOW),
            styled(player.level().dimension().identifier(), YELLOW)
        ).formatted(GREEN).sendTo(player)

        dataManager.save(player.level())

        session.shown.add(propertyId)
        session.destroyEditor()
    }

    fun discard(player: ServerPlayer, nbt: CompoundTag) {
        val session = sessionManager.optSession(player) ?: return

        val editor = session.editor

        if (editor != null) {
            editor.onDataChanged(nbt)

            if (!editor.isNew()) {
                editor.onTerminate(nbt)
            }
        }

        session.destroyEditor()
    }

    fun onClose(player: ServerPlayer, nbt: CompoundTag) {
        val session = sessionManager.optSession(player) ?: return
        val editor = session.editor ?: return

        nbt.getString("propertyId").ifPresent { editor.propertyId = it }

        editor.onDataChanged(nbt)
    }

    companion object {
        val SAVE_ID = identifier("save")
        val DISCARD_ID = identifier("save_discard")
        val CONFIRM_ID = identifier("save_confirm")
        val CLOSE_ID = identifier("save_close")
    }
}