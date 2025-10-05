package work.lclpnet.map_utils.dialog

import net.minecraft.dialog.AfterAction
import net.minecraft.dialog.DialogActionButtonData
import net.minecraft.dialog.DialogButtonData
import net.minecraft.dialog.DialogCommonData
import net.minecraft.dialog.action.DynamicCustomDialogAction
import net.minecraft.dialog.input.SingleOptionInputControl
import net.minecraft.dialog.type.DialogInput
import net.minecraft.dialog.type.MultiActionDialog
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting.*
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.Data
import work.lclpnet.map_utils.data.DataManager
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.openConfirmDialog
import java.util.*

class ListDialog(val translations: Translations, val dataManager: DataManager, val sessionManager: SessionManager) {

    fun open(player: ServerPlayerEntity) {
        val session = sessionManager.getSession(player)
        val worldData = dataManager.getWorldData(player.world)

        val actions = mutableListOf<DialogActionButtonData>()

        val counter = mutableMapOf<Data<*>, Int>()

        for ((propertyId, dataInstance) in worldData.properties()) {
            val nbt = NbtCompound()
            nbt.putString("propertyId", propertyId)

            val num = counter.compute(dataInstance.data) { _, i -> if (i == null) 1 else i + 1 }

            val label = Text.empty()
                .append(Text.literal(propertyId).formatted(YELLOW))
                .append(" (")
                .append(translations.translateText("type.${dataInstance.data.id()}")
                    .formatted(AQUA)
                    .translateFor(player))
                .append(Text.literal(" #$num").formatted(AQUA))
                .append(")")

            actions.add(DialogActionButtonData(
                DialogButtonData(label, 200),
                Optional.of(DynamicCustomDialogAction(SELECT_ID, Optional.of(nbt)))
            ))

            actions.add(DialogActionButtonData(
                DialogButtonData(Text.literal("\uD83D\uDC41")
                    .formatted(if (session.shown.contains(propertyId)) GREEN else STRIKETHROUGH), 20),
                Optional.of(DynamicCustomDialogAction(TOGGLE_SHOWN_ID, Optional.of(nbt)))
            ))

            actions.add(DialogActionButtonData(
                DialogButtonData(Text.literal("↑"), 20),
                Optional.of(DynamicCustomDialogAction(MOVE_UP_ID, Optional.of(nbt)))
            ))

            actions.add(DialogActionButtonData(
                DialogButtonData(Text.literal("↓"), 20),
                Optional.of(DynamicCustomDialogAction(MOVE_DOWN_ID, Optional.of(nbt)))
            ))

            actions.add(DialogActionButtonData(
                DialogButtonData(Text.literal("\uD83D\uDDD1").formatted(RED), 20),
                Optional.of(DynamicCustomDialogAction(DELETE_ID, Optional.of(nbt)))
            ))
        }

        val inputs = listOf(
            DialogInput(
                "showStatus",
                SingleOptionInputControl(
                    150,
                    listOf(
                        SingleOptionInputControl.Entry(
                            "show_all",
                            Optional.of(translations.translateText("list.show_all").translateFor(player)),
                            session.shown.isNotEmpty() && session.shown.size >= worldData.properties().size
                        ),
                        SingleOptionInputControl.Entry(
                            "show_selected",
                            Optional.of(translations.translateText("list.show_selected").translateFor(player)),
                            session.shown.isNotEmpty() && session.shown.size < worldData.properties().size
                        ),
                        SingleOptionInputControl.Entry(
                            "hide_all",
                            Optional.of(translations.translateText("list.hide_all").translateFor(player)),
                            session.shown.isEmpty()
                        )
                    ),
                    translations.translateText("list.show_status").translateFor(player),
                    true
                )
            )
        )

        val title = translations.translateText("list.title", player.world.registryKey.value).translateFor(player)

        val dialog = MultiActionDialog(
            DialogCommonData(
                title, Optional.empty(), true, true, AfterAction.CLOSE, listOf(), inputs
            ),
            actions,
            Optional.of(DialogActionButtonData(
                DialogButtonData(Text.translatable("gui.cancel"), 150),
                Optional.of(DynamicCustomDialogAction(CLOSE_ID, Optional.empty()))
            )),
            5
        )

        player.openDialog(RegistryEntry.of(dialog))
    }

    fun select(player: ServerPlayerEntity, nbt: NbtCompound) {
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

    fun confirmSelect(player: ServerPlayerEntity, nbt: NbtCompound) {
        val propertyId = nbt.getString("propertyId", null) ?: return
        val worldData = dataManager.getWorldData(player.world)
        val dataInstance = worldData[propertyId] ?: return

        val session = sessionManager.getSession(player)
        session.destroyEditor()

        sessionManager.getSession(player).removeSessionDisplay(propertyId)

        val editor = dataInstance.restore(session, propertyId)

        session.setEditor(editor)
    }

    fun delete(player: ServerPlayerEntity, nbt: NbtCompound) {
        onDataChange(player, nbt)

        val propertyId = nbt.getString("propertyId", null) ?: return

        val msg = translations.translateText(
            "list.confirm_delete",
            Text.literal(propertyId).formatted(YELLOW)
        ).formatted(RED).translateFor(player)

        val label = translations.translateText("delete").formatted(RED).translateFor(player)

        openConfirmDialog(player, translations, msg, CONFIRM_DELETE_ID, label, Optional.of(nbt))
    }

    fun confirmDelete(player: ServerPlayerEntity, nbt: NbtCompound) {
        val propertyId = nbt.getString("propertyId", null) ?: return
        val worldData = dataManager.getWorldData(player.world)

        sessionManager.getSession(player).removeSessionDisplay(propertyId)
        worldData.remove(propertyId)

        dataManager.save(player.world)

        translations.translateText(
            "list.deleted",
            Text.literal(propertyId).formatted(YELLOW)
        ).formatted(GREEN).sendTo(player)
    }

    fun moveUp(player: ServerPlayerEntity, nbt: NbtCompound) {
        onDataChange(player, nbt)

        val propertyId = nbt.getString("propertyId", null) ?: return
        val worldData = dataManager.getWorldData(player.world)

        val index = worldData.getIndex(propertyId)

        if (index == -1 || index == 0) {
            open(player)
            return
        }

        worldData.setIndex(propertyId, index - 1)

        dataManager.save(player.world)

        open(player)
    }

    fun moveDown(player: ServerPlayerEntity, nbt: NbtCompound) {
        onDataChange(player, nbt)

        val propertyId = nbt.getString("propertyId", null) ?: return
        val worldData = dataManager.getWorldData(player.world)

        val index = worldData.getIndex(propertyId)

        if (index == -1 || index >= worldData.properties().size - 1) {
            open(player)
            return
        }

        worldData.setIndex(propertyId, index + 1)

        dataManager.save(player.world)

        open(player)
    }

    fun onClose(player: ServerPlayerEntity, nbt: NbtCompound) {
        onDataChange(player, nbt)
    }

    fun toggleShown(player: ServerPlayerEntity, nbt: NbtCompound) {
        val propertyId = nbt.getString("propertyId", null) ?: return
        val session = sessionManager.getSession(player)

        if (session.shown.contains(propertyId)) {
            session.shown.remove(propertyId)
        } else {
            session.shown.add(propertyId)
        }

        session.updateShownDisplays()
        open(player)
    }

    fun showAll(player: ServerPlayerEntity) {
        val session = sessionManager.getSession(player)

        val worldData = dataManager.getWorldData(player.world)

        for ((propertyId, _) in worldData.properties()) {
            session.shown.add(propertyId)
        }

        session.updateShownDisplays()
    }

    fun hideAll(player: ServerPlayerEntity) {
        val session = sessionManager.getSession(player)

        session.shown.clear()
        session.updateShownDisplays()
    }

    private fun onDataChange(player: ServerPlayerEntity, nbt: NbtCompound) {
        val showStatus = nbt.getString("showStatus", "show_selected")

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