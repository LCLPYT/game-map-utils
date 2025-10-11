package work.lclpnet.map_utils.editor

import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.body.PlainMessageDialogBody
import net.minecraft.dialog.type.DialogInput
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.Data
import work.lclpnet.map_api.data.DataInstance
import work.lclpnet.map_api.data.DataManager
import work.lclpnet.map_api.visual.Visualizer

interface DataEditor<T> {
    val args: SessionArgs
    val visualizer: Visualizer
    val id: String
    var propertyId: String?
    var role: String?
    var prevPropertyId: String?

    fun data(): Data<T>

    fun isNew(): Boolean

    fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<DialogInput>,
        translations: Translations,
        player: ServerPlayerEntity
    )

    fun sendTutorial()

    fun init(hooks: HookRegistrar)

    fun load(value: T)

    fun create(nbt: NbtCompound): T?

    fun saveToWorld(world: ServerWorld, dataManager: DataManager, propertyId: String, role: String?, nbt: NbtCompound): Boolean {
        val value = create(nbt) ?: return false
        val instance = DataInstance(data(), value, role)

        dataManager.setDataInstance(world, propertyId, instance)

        return true
    }

    fun <T> required(value: T?, key: String, translations: Translations, player: ServerPlayerEntity, toText: (T) -> Text): PlainMessageDialogBody {
        val detail = if (value != null) toText(value).copy().formatted(Formatting.YELLOW)
        else translations.translateText("required").formatted(Formatting.YELLOW).translateFor(player)

        val text = translations.translateText(player, key(key)).append(": ")
            .formatted(if (value == null) Formatting.RED else Formatting.GREEN)
            .append(detail)

        return messageBody(text)
    }

    fun messageBody(text: Text): PlainMessageDialogBody {
        return PlainMessageDialogBody(text, 200)
    }

    fun key(suffix: String): String = "type.${id}.$suffix"

    fun sendMissing(missing: Set<String>) {
        args.translations.translateText(
            "save.missing",
            Text.literal(missing.joinToString {
                args.translations.translate(args.player(), key(it))
            }).formatted(Formatting.YELLOW)
        ).formatted(Formatting.RED).sendTo(args.player())
    }

    fun onDataChanged(nbt: NbtCompound) {}

    fun onTerminate(nbt: NbtCompound) {}

    val player: ServerPlayerEntity
        get() = args.player()

    val translations: Translations
        get() = args.translations

    val world: ServerWorld
        get() = args.world
}