package work.lclpnet.map_utils.dialog

import net.minecraft.ChatFormatting.RED
import net.minecraft.commands.Commands
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.network.CustomClickActionCallback
import work.lclpnet.kibu.translate.Translations
import java.util.*

class DialogHandler(
    val translations: Translations,
    val createDialog: CreateDialog,
    val saveDialog: SaveDialog,
    val listDialog: ListDialog,
    val schemaSelectorDialog: SchemaSelectorDialog,
    val mapManagerDialog: MapManagerDialog,
) {

    fun init(hooks: HookRegistrar) {
        hooks.registerHook(CustomClickActionCallback.HOOK, CustomClickActionCallback { player, id, payload ->
            onCustomClick(player, id, payload)
        })
    }

    fun onCustomClick(player: ServerPlayer, id: Identifier, payload: Optional<Tag>) {
        if (!Commands.LEVEL_GAMEMASTERS.check(player.permissions()) || player.gameMode().isSurvival) {
            translations.translateText("missing_permission").formatted(RED).sendTo(player)
            return
        }

        val nbt = payload.map { it as? CompoundTag }.orElseGet { CompoundTag() }!!

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
            ListDialog.CLOSE_ID -> listDialog.onClose(player, nbt)
            ListDialog.TOGGLE_SHOWN_ID -> listDialog.toggleShown(player, nbt)
            SchemaSelectorDialog.SELECTOR_ID -> schemaSelectorDialog.open(player)
            SchemaSelectorDialog.SELECT_ID -> schemaSelectorDialog.selectSchema(player, nbt)
            SchemaSelectorDialog.EDIT_PROPERTY_ID -> schemaSelectorDialog.editProperty(player, nbt)
            SchemaSelectorDialog.CONFIRM_EDIT_PROPERTY_ID -> schemaSelectorDialog.confirmEditProperty(player, nbt)
            SchemaSelectorDialog.LIST_PROPERTY_ID -> schemaSelectorDialog.listProperty(player, nbt)
            SchemaSelectorDialog.UNLINK_ID -> schemaSelectorDialog.unlink(player)
            SchemaSelectorDialog.CONFIRM_UNLINK_ID -> schemaSelectorDialog.confirmUnlink(player)
            MapManagerDialog.ID -> mapManagerDialog.open(player, nbt)
            MapManagerDialog.LOAD_ID -> mapManagerDialog.loadWorld(player, nbt)
            MapManagerDialog.CLOSE_ID -> mapManagerDialog.closeWorld(player, nbt)
            MapManagerDialog.TELEPORT_ID -> mapManagerDialog.teleport(player, nbt)
            MapManagerDialog.EXPORT_ID -> mapManagerDialog.exportWorld(player, nbt)
        }
    }
}