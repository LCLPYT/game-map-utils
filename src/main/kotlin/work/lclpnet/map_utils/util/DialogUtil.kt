package work.lclpnet.map_utils.util

import net.minecraft.dialog.AfterAction
import net.minecraft.dialog.DialogActionButtonData
import net.minecraft.dialog.DialogButtonData
import net.minecraft.dialog.DialogCommonData
import net.minecraft.dialog.action.DynamicCustomDialogAction
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.body.PlainMessageDialogBody
import net.minecraft.dialog.type.ConfirmationDialog
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting.*
import net.minecraft.util.Identifier
import work.lclpnet.kibu.translate.Translations
import java.util.*

fun openConfirmDialog(player: ServerPlayerEntity, translations: Translations, msg: Text, confirmId: Identifier) {
    val title = translations.translateText("warning").formatted(YELLOW, BOLD).translateFor(player)
    val discardLabel = translations.translateText("discard").formatted(RED).translateFor(player)

    val body = listOf<DialogBody>(
        PlainMessageDialogBody(msg, 400)
    )

    val dialog = ConfirmationDialog(
        DialogCommonData(
            title, Optional.empty(), true, true, AfterAction.CLOSE, body, listOf()
        ),
        DialogActionButtonData(
            DialogButtonData(discardLabel, 150),
            Optional.of(DynamicCustomDialogAction(confirmId, Optional.empty()))
        ),
        DialogActionButtonData(
            DialogButtonData(Text.translatable("gui.cancel"), 150),
            Optional.empty()
        ),
    )

    player.openDialog(RegistryEntry.of(dialog))
}
