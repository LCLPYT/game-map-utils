package work.lclpnet.map_utils.dialog

import net.minecraft.dialog.AfterAction
import net.minecraft.dialog.DialogActionButtonData
import net.minecraft.dialog.DialogButtonData
import net.minecraft.dialog.DialogCommonData
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.input.SingleOptionInputControl
import net.minecraft.dialog.type.ConfirmationDialog
import net.minecraft.dialog.type.DialogInput
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.DataManager
import work.lclpnet.map_utils.identifier
import java.util.*

class CreateDialog(val translations: Translations, val dataManager: DataManager) {

    fun open(player: ServerPlayerEntity) {
        val title = translations.translateText("create").translateFor(player)

        val body = listOf<DialogBody>()

        val inputs = listOf(typeInput(player))

        val dialog = ConfirmationDialog(
            DialogCommonData(
                title, Optional.empty(), true, true, AfterAction.CLOSE, body, inputs
            ),
            DialogActionButtonData(
                DialogButtonData(Text.translatable("gui.cancel"), 150),
                Optional.empty()
            ),
            DialogActionButtonData(
                DialogButtonData(translations.translateText("create.create").translateFor(player), 150),
                Optional.empty()
            )
        )

        player.openDialog(RegistryEntry.of(dialog))
    }

    fun typeInput(player: ServerPlayerEntity): DialogInput {
        val types = dataManager.types.map { data ->
            val id = data.id()
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

    companion object {
        val ID = identifier("create")
    }
}