package work.lclpnet.map_utils.editor.type

import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.type.DialogInput
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Formatting.GREEN
import net.minecraft.util.Formatting.YELLOW
import net.minecraft.util.math.Vec3d
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks
import work.lclpnet.kibu.hook.util.PositionRotation
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.kibu.translate.text.LocalizedFormat.format
import work.lclpnet.map_utils.data.type.PositionData
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.util.Visualizer
import work.lclpnet.map_utils.util.keybind

class PositionEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null
) : BaseDataEditor<PositionRotation>(PositionData) {

    var posRot: PositionRotation? = null
    var marker: Removable? = null

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<DialogInput>,
        translations: Translations,
        player: ServerPlayerEntity
    ) {
        val locale = translations.getLocale(player)

        body.add(required(posRot, "pos", translations, player) {
            Vec3d(it.x, it.y, it.z).toLocalizedShortString()
                .translateTo(translations.getLanguage(player))
        })

        body.add(required(posRot, "yaw", translations, player) {
            Text.literal(String.format(locale, "%.2f", it.yaw))
        })

        body.add(required(posRot, "pitch", translations, player) {
            Text.literal(String.format(locale, "%.2f", it.pitch))
        })
    }

    override fun init(hooks: HookRegistrar) {
        args.translations.translateText(
            key("init"),
            keybind("sprint", "swapOffhand").formatted(YELLOW),
            keybind("swapOffhand").formatted(YELLOW)
        ).formatted(Formatting.AQUA).sendTo(args.player())

        hooks.registerHook(PlayerInventoryHooks.SWAP_HANDS, PlayerInventoryHooks.SwapHands { player, _ ->
            onSwapHands(player)
        })
    }

    private fun onSwapHands(player: ServerPlayerEntity): Boolean {
        if (player != args.player() || player.world != args.world || !args.player().playerInput.sprint) return false

        val posRot = PositionRotation(player.x, player.y, player.z, player.yaw, player.pitch)
        this.posRot = posRot

        args.translations.translateText(
            key("changed"),
            styled(player.pos.toLocalizedShortString(), YELLOW),
            styled(format("%.2f", player.yaw), YELLOW),
            styled(format("%.2f", player.pitch), YELLOW)
        ).formatted(GREEN).sendTo(player)

        marker?.remove()
        marker = data.display(posRot, visualizer, args.player(), args.translations, prevPropertyId)

        return true
    }

    override fun load(value: PositionRotation) {
        posRot = value

        marker?.remove()
        marker = data.display(value, visualizer, args.player(), args.translations, prevPropertyId)
    }

    override fun create(nbt: NbtCompound): PositionRotation? = posRot
}

private fun Vec3d.toLocalizedShortString() = format("%.2f, %.2f, %.2f", x, y, z)