package work.lclpnet.map_utils.dialog

import net.minecraft.dialog.AfterAction
import net.minecraft.dialog.DialogActionButtonData
import net.minecraft.dialog.DialogButtonData
import net.minecraft.dialog.DialogCommonData
import net.minecraft.dialog.action.DynamicCustomDialogAction
import net.minecraft.dialog.type.MultiActionDialog
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting.*
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.DataManager
import work.lclpnet.map_utils.editor.SessionManager
import work.lclpnet.map_utils.identifier
import work.lclpnet.map_utils.util.openConfirmDialog
import java.util.*

class ListDialog(val translations: Translations, val dataManager: DataManager, val sessionManager: SessionManager) {

    fun open(player: ServerPlayerEntity) {
        val worldData = dataManager.getWorldData(player.world)

        val actions = mutableListOf<DialogActionButtonData>()

        for ((propertyId, dataInstance) in worldData.properties()) {
            val nbt = NbtCompound()
            nbt.putString("propertyId", propertyId)

            val label = Text.empty()
                .append(Text.literal(propertyId).formatted(YELLOW))
                .append(" (")
                .append(translations.translateText("type.${dataInstance.data.id()}")
                    .formatted(AQUA)
                    .translateFor(player))
                .append(")")

            actions.add(DialogActionButtonData(
                DialogButtonData(label, 200),
                Optional.of(DynamicCustomDialogAction(SELECT_ID, Optional.of(nbt)))
            ))

            actions.add(DialogActionButtonData(
                DialogButtonData(Text.literal("\uD83D\uDDD1").formatted(RED), 20),
                Optional.of(DynamicCustomDialogAction(DELETE_ID, Optional.of(nbt)))
            ))
        }

        val title = translations.translateText("list.title", player.world.registryKey.value).translateFor(player)

        val dialog = MultiActionDialog(
            DialogCommonData(
                title, Optional.empty(), true, true, AfterAction.CLOSE, listOf(), listOf()
            ),
            actions,
            Optional.of(DialogActionButtonData(
                DialogButtonData(Text.translatable("gui.close"), 150),
                Optional.empty()
            )),
            2
        )

        player.openDialog(RegistryEntry.of(dialog))
    }

    fun select(player: ServerPlayerEntity, nbt: NbtCompound) {
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

        val editor = dataInstance.restore(session)
        editor.propertyId = propertyId
        editor.prevPropertyId = propertyId

        session.setEditor(editor)
    }

    fun delete(player: ServerPlayerEntity, nbt: NbtCompound) {
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

        worldData.remove(propertyId)

        translations.translateText(
            "list.deleted",
            Text.literal(propertyId).formatted(YELLOW)
        ).formatted(GREEN).sendTo(player)
    }

    companion object {
        val LIST_ID = identifier("list")
        val SELECT_ID = identifier("list_select")
        val CONFIRM_SELECT_ID = identifier("list_confirm_select")
        val DELETE_ID = identifier("list_delete")
        val CONFIRM_DELETE_ID = identifier("list_confirm_delete")
    }
}