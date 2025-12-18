package work.lclpnet.map_utils.dialog

import net.minecraft.ChatFormatting.*
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.*
import net.minecraft.server.dialog.action.CustomAll
import net.minecraft.server.dialog.body.PlainMessage
import net.minecraft.server.dialog.input.SingleOptionInput
import net.minecraft.server.level.ServerPlayer
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.openConfirmDialog
import java.util.*

class ListDialog(val translations: Translations, val dataManager: DataManager, val sessionManager: SessionManager) {

    fun open(player: ServerPlayer) {
        val session = sessionManager.getSession(player)
        val worldData = dataManager.getWorldData(player.level())

        val actions = mutableListOf<ActionButton>()

        val counter = mutableMapOf<Data<*>, Int>()

        for ((propertyId, dataInstance) in worldData.properties()) {
            val nbt = CompoundTag()
            nbt.putString("propertyId", propertyId)

            val num = counter.compute(dataInstance.data) { _, i -> if (i == null) 1 else i + 1 }

            val label = Component.empty()
                .append(Component.literal(propertyId).withStyle(YELLOW))
                .append(" (")
                .append(translations.translateText("type.${dataInstance.data.id()}")
                    .formatted(AQUA)
                    .translateFor(player))
                .append(Component.literal(" #$num").withStyle(YELLOW))
                .append(")")

            actions.add(
                ActionButton(
                CommonButtonData(label, 200),
                Optional.of(CustomAll(SELECT_ID, Optional.of(nbt)))
            ))

            actions.add(
                ActionButton(
                CommonButtonData(
                    Component.literal("\uD83D\uDC41")
                    .withStyle(if (session.shown.contains(propertyId)) GREEN else STRIKETHROUGH), 20),
                Optional.of(CustomAll(TOGGLE_SHOWN_ID, Optional.of(nbt)))
            ))

            actions.add(
                ActionButton(
                CommonButtonData(Component.literal("↑"), 20),
                Optional.of(CustomAll(MOVE_UP_ID, Optional.of(nbt)))
            ))

            actions.add(
                ActionButton(
                CommonButtonData(Component.literal("↓"), 20),
                Optional.of(CustomAll(MOVE_DOWN_ID, Optional.of(nbt)))
            ))

            actions.add(
                ActionButton(
                CommonButtonData(Component.literal("\uD83D\uDDD1").withStyle(RED), 20),
                Optional.of(CustomAll(DELETE_ID, Optional.of(nbt)))
            ))
        }

        val visibilityOptions = mutableListOf<SingleOptionInput.Entry>()
        val allShown = session.shown.isNotEmpty() && session.shown.size >= worldData.properties().size

        visibilityOptions.add(
            SingleOptionInput.Entry(
            "show_all",
            Optional.of(translations.translateText("list.show_all").translateFor(player)),
            allShown
        ))

        if (!allShown && !session.shown.isEmpty()) {
            visibilityOptions.add(
                SingleOptionInput.Entry(
                "show_selected",
                Optional.of(translations.translateText("list.show_selected").translateFor(player)),
                session.shown.isNotEmpty() && session.shown.size < worldData.properties().size
            ))
        }

        visibilityOptions.add(
            SingleOptionInput.Entry(
            "hide_all",
            Optional.of(translations.translateText("list.hide_all").translateFor(player)),
            session.shown.isEmpty()
        ))

        val inputs = listOf(
            Input(
                "showStatus",
                SingleOptionInput(
                    150,
                    visibilityOptions,
                    translations.translateText("list.show_status").translateFor(player),
                    true
                )
            )
        )

        val title = translations.translateText("list.title", player.level().dimension().identifier()).translateFor(player)

        val commonData = CommonDialogData(
            title,
            Optional.empty(),
            true,
            true,
            DialogAction.CLOSE,
            if (actions.isNotEmpty()) listOf() else listOf(
                PlainMessage(
                translations.translateText("list.no_data").formatted(YELLOW).translateFor(player),
                200
            )),
            if (actions.isNotEmpty()) inputs else listOf()
        )

        val cancelButton = ActionButton(
            CommonButtonData(Component.translatable("gui.cancel"), 150),
            Optional.of(CustomAll(CLOSE_ID, Optional.empty()))
        )

        val dialog = if (actions.isNotEmpty()) MultiActionDialog(
            commonData,
            actions,
            Optional.of(cancelButton),
            5
        ) else MultiActionDialog(
            commonData,
            listOf(
                ActionButton(
                CommonButtonData(translations.translateText("editor.create_data").translateFor(player), 150),
                Optional.of(CustomAll(CreateDialog.OPEN_ID, Optional.empty()))
            )),
            Optional.of(cancelButton),
            1
        )

        player.openDialog(Holder.direct(dialog))
    }

    fun select(player: ServerPlayer, nbt: CompoundTag) {
        onDataChange(player, nbt)

        val session = sessionManager.getSession(player)
        
        if (session.editor != null) {
            val msg = translations.translateText("list.replace_editor")
                .formatted(YELLOW)
                .translateFor(player)

            openConfirmDialog(player, translations, msg, CONFIRM_SELECT_ID, payload = Optional.of(nbt))
            return
        }

        confirmSelect(player, nbt)
    }

    fun confirmSelect(player: ServerPlayer, nbt: CompoundTag) {
        val propertyId = nbt.getString("propertyId").orElse(null) ?: return
        val worldData = dataManager.getWorldData(player.level())
        val dataInstance = worldData[propertyId] ?: return

        val session = sessionManager.getSession(player)
        session.destroyEditor()

        sessionManager.getSession(player).removeSessionDisplay(propertyId)

        val editor = session.createEditorFrom(dataInstance, propertyId)

        session.setEditor(editor)
    }

    fun delete(player: ServerPlayer, nbt: CompoundTag) {
        onDataChange(player, nbt)

        val propertyId = nbt.getString("propertyId").orElse(null) ?: return

        val msg = translations.translateText(
            "list.confirm_delete",
            Component.literal(propertyId).withStyle(YELLOW)
        ).formatted(RED).translateFor(player)

        val label = translations.translateText("delete").formatted(RED).translateFor(player)

        openConfirmDialog(player, translations, msg, CONFIRM_DELETE_ID, label, Optional.of(nbt))
    }

    fun confirmDelete(player: ServerPlayer, nbt: CompoundTag) {
        val propertyId = nbt.getString("propertyId").orElse(null) ?: return
        val worldData = dataManager.getWorldData(player.level())

        val session = sessionManager.getSession(player)

        session.removeSessionDisplay(propertyId)
        worldData.remove(propertyId)

        dataManager.save(player.level())

        translations.translateText(
            "list.deleted",
            Component.literal(propertyId).withStyle(YELLOW)
        ).formatted(GREEN).sendTo(player)

        if (session.editor?.propertyId == propertyId) {
            session.destroyEditor()
        }
    }

    fun moveUp(player: ServerPlayer, nbt: CompoundTag) {
        onDataChange(player, nbt)

        val propertyId = nbt.getString("propertyId").orElse(null) ?: return
        val worldData = dataManager.getWorldData(player.level())

        val index = worldData.getIndex(propertyId)

        if (index == -1 || index == 0) {
            open(player)
            return
        }

        worldData.setIndex(propertyId, index - 1)

        dataManager.save(player.level())

        open(player)
    }

    fun moveDown(player: ServerPlayer, nbt: CompoundTag) {
        onDataChange(player, nbt)

        val propertyId = nbt.getString("propertyId").orElse(null) ?: return
        val worldData = dataManager.getWorldData(player.level())

        val index = worldData.getIndex(propertyId)

        if (index == -1 || index >= worldData.properties().size - 1) {
            open(player)
            return
        }

        worldData.setIndex(propertyId, index + 1)

        dataManager.save(player.level())

        open(player)
    }

    fun onClose(player: ServerPlayer, nbt: CompoundTag) {
        onDataChange(player, nbt)
    }

    fun toggleShown(player: ServerPlayer, nbt: CompoundTag) {
        val propertyId = nbt.getString("propertyId").orElse(null) ?: return
        val session = sessionManager.getSession(player)

        if (session.shown.contains(propertyId)) {
            session.shown.remove(propertyId)
        } else {
            session.shown.add(propertyId)
        }

        session.updateShownDisplays()
        open(player)
    }

    fun showAll(player: ServerPlayer) {
        val session = sessionManager.getSession(player)

        val worldData = dataManager.getWorldData(player.level())

        for ((propertyId, _) in worldData.properties()) {
            session.shown.add(propertyId)
        }

        session.updateShownDisplays()
    }

    fun hideAll(player: ServerPlayer) {
        val session = sessionManager.getSession(player)

        session.shown.clear()
        session.updateShownDisplays()
    }

    private fun onDataChange(player: ServerPlayer, nbt: CompoundTag) {
        val showStatus = nbt.getStringOr("showStatus", "show_selected")

        when (showStatus) {
            "show_all" -> showAll(player)
            "show_selected" -> sessionManager.getSession(player).updateShownDisplays()
            "hide_all" -> hideAll(player)
        }
    }

    companion object {
        val LIST_ID = identifier("list")
        val SELECT_ID = identifier("list_select")
        val CONFIRM_SELECT_ID = identifier("list_confirm_select")
        val DELETE_ID = identifier("list_delete")
        val CONFIRM_DELETE_ID = identifier("list_confirm_delete")
        val MOVE_UP_ID = identifier("list_move_up")
        val MOVE_DOWN_ID = identifier("list_move_down")
        val CLOSE_ID = identifier("list_close")
        val TOGGLE_SHOWN_ID = identifier("list_toggle_shown")
    }
}