package work.lclpnet.map_utils.util

import net.minecraft.ChatFormatting.*
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.dialog.*
import net.minecraft.server.dialog.action.CustomAll
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.dialog.body.PlainMessage
import net.minecraft.server.level.ServerPlayer
import work.lclpnet.kibu.translate.Translations
import java.util.*

fun openConfirmDialog(
    player: ServerPlayer,
    translations: Translations,
    msg: Component,
    confirmId: Identifier,
    confirmLabel: Component = translations.translateText("discard").withStyle(RED).translateFor(player),
    payload: Optional<CompoundTag> = Optional.empty()
) {
    val title = translations.translateText("warning").withStyle(YELLOW, BOLD).translateFor(player)

    val body = listOf<DialogBody>(
        PlainMessage(msg, 400)
    )

    val dialog = ConfirmationDialog(
        CommonDialogData(
            title, Optional.empty(), true, true, DialogAction.CLOSE, body, listOf()
        ),
        ActionButton(
            CommonButtonData(confirmLabel, 150),
            Optional.of(CustomAll(confirmId, payload))
        ),
        ActionButton(
            CommonButtonData(Component.translatable("gui.cancel"), 150),
            Optional.empty()
        ),
    )

    player.openDialog(Holder.direct(dialog))
}
