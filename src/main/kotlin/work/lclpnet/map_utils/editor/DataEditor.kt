package work.lclpnet.map_utils.editor

import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.Input
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.dialog.body.PlainMessage
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
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
    var prevPropertyId: String?
    var role: String?

    fun data(): Data<T>

    fun isNew(): Boolean

    fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<Input>,
        translations: Translations,
        player: ServerPlayer
    )

    fun sendTutorial()

    fun init(hooks: HookRegistrar)

    fun load(value: T)

    fun create(nbt: CompoundTag): T?

    fun saveToWorld(world: ServerLevel, dataManager: DataManager, propertyId: String, role: String?, nbt: CompoundTag): Boolean {
        val value = create(nbt) ?: return false
        val instance = DataInstance(data(), value, role)

        dataManager.setDataInstance(world, propertyId, instance)

        return true
    }

    fun <T> required(value: T?, key: String, translations: Translations, player: ServerPlayer, toText: (T) -> Component): PlainMessage {
        val detail = if (value != null) toText(value).copy().withStyle(ChatFormatting.YELLOW)
        else translations.translateText("required").formatted(ChatFormatting.YELLOW).translateFor(player)

        val text = translations.translateText(player, key(key)).append(": ")
            .formatted(if (value == null) ChatFormatting.RED else ChatFormatting.GREEN)
            .append(detail)

        return messageBody(text)
    }

    fun messageBody(text: Component): PlainMessage {
        return PlainMessage(text, 200)
    }

    fun key(suffix: String): String = "type.${id}.$suffix"

    fun sendMissing(missing: Set<String>) {
        args.translations.translateText(
            "save.missing",
            Component.literal(missing.joinToString {
                args.translations.translate(args.player(), key(it))
            }).withStyle(ChatFormatting.YELLOW)
        ).formatted(ChatFormatting.RED).sendTo(args.player())
    }

    fun onDataChanged(nbt: CompoundTag) {}

    fun onTerminate(nbt: CompoundTag) {}

    val player: ServerPlayer
        get() = args.player()

    val translations: Translations
        get() = args.translations

    val world: ServerLevel
        get() = args.world
}