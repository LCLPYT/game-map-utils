package work.lclpnet.map_utils.data

import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.body.PlainMessageDialogBody
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.editor.SessionArgs

interface Data {
    fun id(): String
    fun createEditor(propertyId: String?): DataEditor
}

interface DataEditor {
    fun data(): Data
    fun propertyId(): String?
    fun addBody(body: MutableList<DialogBody>, translations: Translations, player: ServerPlayerEntity)

    fun init(hooks: HookRegistrar, args: SessionArgs)

    fun <T> required(value: T?, key: String, translations: Translations, player: ServerPlayerEntity, toText: (T) -> Text): PlainMessageDialogBody {
        val detail = if (value != null) toText(value).copy().formatted(Formatting.YELLOW)
        else translations.translateText("required").formatted(Formatting.YELLOW).translateFor(player)

        val text = translations.translateText("type.${data().id()}.$key", detail as Object)
            .formatted(if (value == null) Formatting.RED else Formatting.GREEN)
            .translateFor(player)

        return PlainMessageDialogBody(text, 200)
    }
}