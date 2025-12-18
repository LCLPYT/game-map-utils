package work.lclpnet.map_utils.editor.type

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.ChatFormatting
import net.minecraft.ChatFormatting.BLUE
import net.minecraft.ChatFormatting.RED
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.Input
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import work.lclpnet.gaco.ds.BlockBox
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.map_api.data.type.BlockBoxData
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.keybind

class BlockBoxEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = BlockBoxData.id(),
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null,
    override var role: String? = null,
) : BaseDataEditor<BlockBox>(BlockBoxData) {

    var pos1: BlockPos? = null
    var pos2: BlockPos? = null

    var pos1Marker: Display.BlockDisplay? = null
    var pos2Marker: Display.BlockDisplay? = null
    var boxMarker: Removable? = null

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<Input>,
        translations: Translations,
        player: ServerPlayer
    ) {
        body.add(required(pos1, "pos1", translations, player) { Component.literal(it.toShortString()) })
        body.add(required(pos2, "pos2", translations, player) { Component.literal(it.toShortString()) })
    }

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            translations.translateText(key("pos1"))
                .formatted(BLUE)
                .translateFor(player),
            keybind("sprint", "attack").withStyle(ChatFormatting.YELLOW),
            translations.translateText(key("pos2"))
                .formatted(RED)
                .translateFor(player),
            keybind("sprint", "use").withStyle(ChatFormatting.YELLOW),
            keybind("swapOffhand").withStyle(ChatFormatting.YELLOW)
        ).formatted(ChatFormatting.AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInteractionHooks.ATTACK_BLOCK, AttackBlockCallback { entity, world, _, pos, _ ->
            attackBlock(entity, world, pos)
        })

        hooks.registerHook(PlayerInteractionHooks.USE_BLOCK, UseBlockCallback { entity, world, hand, result ->
            useBlock(entity, world, hand, result)
        })
    }

    private fun useBlock(
        entity: Player,
        world: Level,
        hand: InteractionHand,
        result: BlockHitResult,
    ): InteractionResult {
        if (entity != player || world != this.world || !player.lastClientInput.sprint || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS

        val pos = result.blockPos

        pos2 = pos.immutable()
        sendPosChanged(key("set_pos2"), pos)

        pos2Marker = updatePosMarker(pos, world, pos2Marker, DyeColor.RED.textureDiffuseColor)

        updateBox()

        return InteractionResult.FAIL
    }

    fun attackBlock(
        entity: Player,
        world: Level,
        pos: BlockPos,
    ): InteractionResult {
        if (entity != player || world != this.world || !player.lastClientInput.sprint) return InteractionResult.PASS

        pos1 = pos.immutable()
        sendPosChanged(key("set_pos1"), pos)

        pos1Marker = updatePosMarker(pos, world, pos1Marker, DyeColor.BLUE.textureDiffuseColor)

        updateBox()

        return InteractionResult.FAIL
    }

    private fun updatePosMarker(pos: BlockPos, world: Level, marker: Display.BlockDisplay?, color: Int): Display.BlockDisplay {
        if (marker != null) {
            visualizer.removeEntity(marker)
        }

        return visualizer.markBlock(pos, world.getBlockState(pos), color)
    }

    private fun sendPosChanged(key: String, pos: BlockPos) {
        translations.translateText(
            key,
            styled(pos.toShortString(), ChatFormatting.YELLOW)
        ).formatted(ChatFormatting.GREEN).sendTo(player)
    }

    private fun updateBox() {
        val pos1 = pos1
        val pos2 = pos2

        if (pos1 == null || pos2 == null) return

        val box = BlockBox(pos1, pos2)

        boxMarker?.remove()

        boxMarker = data.display(box, visualizer, player, translations, id, propertyId)
    }

    override fun create(nbt: CompoundTag): BlockBox? {
        val pos1 = this.pos1
        val pos2 = this.pos2

        val missing = mutableSetOf<String>()

        if (pos1 == null) missing.add("pos1")
        if (pos2 == null) missing.add("pos2")

        if (missing.isNotEmpty()) {
            sendMissing(missing)
            return null
        }

        return BlockBox(pos1, pos2)
    }

    override fun load(value: BlockBox) {
        pos1 = value.min()
        pos2 = value.max()

        pos1Marker = updatePosMarker(value.min(), world, pos1Marker, DyeColor.BLUE.textureDiffuseColor)
        pos2Marker = updatePosMarker(value.max(), world, pos2Marker, DyeColor.RED.textureDiffuseColor)

        updateBox()
    }
}
