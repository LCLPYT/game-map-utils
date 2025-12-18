package work.lclpnet.map_utils.editor.type

import net.minecraft.ChatFormatting
import net.minecraft.ChatFormatting.GREEN
import net.minecraft.ChatFormatting.YELLOW
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.Input
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks
import work.lclpnet.kibu.hook.util.PositionRotation
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.kibu.translate.text.LocalizedFormat.format
import work.lclpnet.map_api.data.type.PositionData
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.keybind
import work.lclpnet.map_utils.util.toLocalizedShortString

class PositionEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = PositionData.id(),
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null,
    override var role: String? = null,
) : BaseDataEditor<PositionRotation>(PositionData) {

    var posRot: PositionRotation? = null
    var marker: Removable? = null

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<Input>,
        translations: Translations,
        player: ServerPlayer
    ) {
        val locale = translations.getLocale(player)

        body.add(required(posRot, "pos", translations, player) {
            Vec3(it.x(), it.y(), it.z()).toLocalizedShortString()
                .translateTo(translations.getLanguage(player))
        })

        body.add(required(posRot, "yaw", translations, player) {
            Component.literal(String.format(locale, "%.2f", it.yaw))
        })

        body.add(required(posRot, "pitch", translations, player) {
            Component.literal(String.format(locale, "%.2f", it.pitch))
        })
    }

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            keybind("sprint", "swapOffhand").withStyle(YELLOW),
            keybind("swapOffhand").withStyle(YELLOW)
        ).formatted(ChatFormatting.AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInventoryHooks.SWAP_HANDS, PlayerInventoryHooks.SwapHands { player, _ ->
            onSwapHands(player)
        })
    }

    private fun onSwapHands(player: ServerPlayer): Boolean {
        if (player != this.player || player.level() != world || !player.lastClientInput.sprint) return false

        val posRot = PositionRotation(player.x, player.y, player.z, player.yRot, player.xRot)
        this.posRot = posRot

        translations.translateText(
            key("set_pos"),
            styled(player.position().toLocalizedShortString(), YELLOW),
            styled(format("%.2f", player.yRot), YELLOW),
            styled(format("%.2f", player.xRot), YELLOW)
        ).formatted(GREEN).sendTo(player)

        marker?.remove()
        marker = data.display(posRot, visualizer, player, translations, id, propertyId)

        return true
    }

    override fun load(value: PositionRotation) {
        posRot = value

        marker?.remove()
        marker = data.display(value, visualizer, player, translations, id, propertyId)
    }

    override fun create(nbt: CompoundTag): PositionRotation? {
        if (posRot == null) {
            sendMissing(setOf("pos"))
        }

        return posRot
    }
}