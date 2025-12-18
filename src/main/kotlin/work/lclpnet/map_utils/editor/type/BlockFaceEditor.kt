package work.lclpnet.map_utils.editor.type

import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.Input
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
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
        inputs: MutableList<Input>,
        translations: Translations,
        player: ServerPlayer
    ) {
        body.add(required(pos, "pos", translations, player) { Component.literal(it.toShortString()) })
        body.add(required(pos, "face", translations, player) { Component.literal(it.toShortString()) })
    }

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            keybind("sprint", "use").withStyle(ChatFormatting.YELLOW),
            keybind("swapOffhand").withStyle(ChatFormatting.YELLOW)
        ).formatted(ChatFormatting.AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInteractionHooks.USE_BLOCK, UseBlockCallback { entity, world, hand, result ->
            useBlock(entity, world, hand, result)
        })
    }

    private fun useBlock(
        entity: Player,
        world: Level,
        hand: InteractionHand,
        result: BlockHitResult
    ): InteractionResult {
        if (entity != player || world != this.world || !player.lastClientInput.sprint || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS

        val pos = result.blockPos
        val face = result.direction
        this.pos = pos
        this.face = face

        updateMarker()

        translations.translateText(
            key("set_pos"),
            styled(pos.toShortString(), ChatFormatting.YELLOW),
            styled(
                face.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
                }, ChatFormatting.YELLOW),
        ).formatted(ChatFormatting.GREEN).sendTo(player)

        return InteractionResult.FAIL
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

    override fun create(nbt: CompoundTag): BlockFace? {
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