package work.lclpnet.map_utils.data

import com.mojang.serialization.Codec
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.body.PlainMessageDialogBody
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.editor.Visualizer

interface Data<T> {
    fun id(): String
    fun codec(): Codec<T>
    fun createEditor(args: SessionArgs, visualizer: Visualizer, propertyId: String?): DataEditor<T>
}

interface DataEditor<T> {
    val args: SessionArgs
    val visualizer: Visualizer
    var propertyId: String?
    var prevPropertyId: String?

    fun data(): Data<T>

    fun isNew(): Boolean

    fun addBody(body: MutableList<DialogBody>, translations: Translations, player: ServerPlayerEntity)

    fun init(hooks: HookRegistrar)

    fun create(): T?

    fun saveToWorld(world: ServerWorld, dataManager: DataManager, propertyId: String): Boolean {
        val value = create() ?: return false

        dataManager.setData(world, propertyId, data(), value)

        return true
    }

    fun <T> required(value: T?, key: String, translations: Translations, player: ServerPlayerEntity, toText: (T) -> Text): PlainMessageDialogBody {
        val detail = if (value != null) toText(value).copy().formatted(Formatting.YELLOW)
        else translations.translateText("required").formatted(Formatting.YELLOW).translateFor(player)

        val text = translations.translateText(player, "type.${data().id()}.$key").append(": ")
            .formatted(if (value == null) Formatting.RED else Formatting.GREEN)
            .append(detail)

        return messageBody(text)
    }

    fun messageBody(text: Text): PlainMessageDialogBody {
        return PlainMessageDialogBody(text, 200)
    }
}