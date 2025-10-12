package work.lclpnet.map_utils.editor.type

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.type.DialogInput
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import work.lclpnet.gaco.math.BlockFace
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.map_api.data.type.BlockFaceData
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.keybind
import java.util.Locale.getDefault

class BlockFaceEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = BlockFaceData.id(),
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null,
    override var role: String? = null,
) : BaseDataEditor<BlockFace>(BlockFaceData) {

    var pos: BlockPos? = null
    var face: Direction? = null
    var marker: Removable? = null

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<DialogInput>,
        translations: Translations,
        player: ServerPlayerEntity
    ) {
        body.add(required(pos, "pos", translations, player) { Text.literal(it.toShortString()) })
        body.add(required(pos, "face", translations, player) { Text.literal(it.toShortString()) })
    }

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            keybind("sprint", "use").formatted(Formatting.YELLOW),
            keybind("swapOffhand").formatted(Formatting.YELLOW)
        ).formatted(Formatting.AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInteractionHooks.USE_BLOCK, UseBlockCallback { entity, world, hand, result ->
            useBlock(entity, world, hand, result)
        })
    }

    private fun useBlock(
        entity: PlayerEntity,
        world: World,
        hand: Hand,
        result: BlockHitResult
    ): ActionResult {
        if (entity != player || world != this.world || !player.playerInput.sprint || hand != Hand.MAIN_HAND) return ActionResult.PASS

        val pos = result.blockPos
        val face = result.side
        this.pos = pos
        this.face = face

        updateMarker()

        translations.translateText(
            key("set_pos"),
            styled(pos.toShortString(), Formatting.YELLOW),
            styled(
                face.id.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
                }, Formatting.YELLOW),
        ).formatted(Formatting.GREEN).sendTo(player)

        return ActionResult.FAIL
    }

    override fun load(value: BlockFace) {
        pos = value.pos
        face = value.face

        updateMarker()
    }

    private fun updateMarker() {
        marker?.remove()

        val pos = this.pos ?: return
        val face = this.face ?: return

        marker = BlockFaceData.display(BlockFace(pos, face), visualizer, player, translations, id, propertyId)
    }

    override fun create(nbt: NbtCompound): BlockFace? {
        val pos = pos
        val face = face

        val missing = mutableSetOf<String>()

        if (pos == null) missing.add("pos")
        if (face == null) missing.add("face")

        if (missing.isNotEmpty()) {
            sendMissing(missing)
            return null
        }

        return BlockFace(pos, face)
    }
}