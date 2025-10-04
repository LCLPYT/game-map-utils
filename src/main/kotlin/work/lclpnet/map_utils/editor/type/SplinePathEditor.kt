package work.lclpnet.map_utils.editor.type

import net.minecraft.block.Blocks
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.type.DialogInput
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting.*
import net.minecraft.util.math.Vec3d
import work.lclpnet.gaco.math.SplinePath
import work.lclpnet.gaco.scene.Object3d
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.map_utils.LOGGER
import work.lclpnet.map_utils.data.type.SplinePathData
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.util.keybind
import work.lclpnet.map_utils.util.toLocalizedShortString
import work.lclpnet.map_utils.visual.Visualizer

class SplinePathEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = SplinePathData.id(),
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null
) : BaseDataEditor<SplinePath>(SplinePathData) {

    val keypoints = mutableListOf<Vec3d>()
    var pathDisplay: Removable? = null
    var selectedIndex = -1

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<DialogInput>,
        translations: Translations,
        player: ServerPlayerEntity
    ) {
        body.add(messageBody(translations.translateText(player, key("header"), keypoints.size)))

        if (keypoints.size < 2) {
            body.add(messageBody(translations.translateText(key("too_few_keypoints"))
                .formatted(RED)
                .translateFor(player)))
        }
    }

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            keybind("sprint", "swapOffhand").formatted(YELLOW),
            keybind("sprint", "use").formatted(YELLOW),
            keybind("sprint", "attack").formatted(YELLOW),
            keybind("swapOffhand").formatted(YELLOW)
        ).formatted(AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInventoryHooks.SWAP_HANDS, PlayerInventoryHooks.SwapHands { player, _ ->
            onSwapHands(player)
        })
    }

    private fun onSwapHands(player: ServerPlayerEntity): Boolean {
        if (player != this.player || player.world != args.world || !player.playerInput.sprint) return false

        addKeypoint(player.pos)

        translations.translateText(
            key("keypoint_added"),
            styled(keypoints.size, YELLOW),
            styled(player.pos.toLocalizedShortString(), YELLOW)
        ).formatted(GREEN).sendTo(player)

        return true
    }

    private fun addKeypoint(pos: Vec3d) {
        selectedIndex++
        keypoints.add(selectedIndex, pos)

        updateDisplay()
        return
    }

    private fun updateDisplay() {
        pathDisplay?.remove()

        if (keypoints.size >= 2) {
            val path = SplinePath.create(keypoints, LOGGER).orElse(null) ?: return

            pathDisplay = data.display(path, visualizer, player, translations, id, prevPropertyId)
            return
        }

        val markers = mutableListOf<Object3d>()

        for (keypoint in keypoints) {
            markers.add(visualizer.marker(keypoint, Blocks.ORANGE_CONCRETE.defaultState, 0xeeff00, 0.5))
        }

        pathDisplay = Removable {
            markers.forEach { it.detach() }
            markers.clear()
        }
    }

    override fun load(value: SplinePath) {
        selectedIndex = -1
        keypoints.clear()

        keypoints.addAll(value.keypoints)

        updateDisplay()
    }

    override fun create(nbt: NbtCompound): SplinePath? {
        if (keypoints.size < 2) {
            translations.translateText(key("too_few_keypoints"))
                .formatted(RED)
                .sendTo(player)
            return null
        }
        
        return SplinePath.create(keypoints, LOGGER).orElse(null)
    }
}