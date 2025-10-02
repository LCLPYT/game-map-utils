package work.lclpnet.map_utils.editor.type

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.BlockState
import net.minecraft.dialog.body.DialogBody
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.DyeColor
import net.minecraft.util.Formatting.*
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.type.PositionedBlockSetData
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.editor.Visualizer
import work.lclpnet.map_utils.util.PositionedBlockSet
import work.lclpnet.map_utils.util.keybind

class PositionedBlockSetEditor(
    data: PositionedBlockSetData,
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override var propertyId: String?,
    override var prevPropertyId: String?
) : BaseDataEditor<PositionedBlockSet>(data) {

    val blocks = mutableMapOf<BlockPos, BlockState>()
    val markers = mutableMapOf<BlockPos, DisplayEntity.BlockDisplayEntity>()

    override fun addBody(
        body: MutableList<DialogBody>,
        translations: Translations,
        player: ServerPlayerEntity
    ) {
        body.add(messageBody(translations.translateText(player, "type.positioned_block_set.header", blocks.size)))

        for ((pos, state) in blocks) {
            body.add(messageBody(label(pos, state)))
        }
    }

    private fun label(pos: BlockPos, state: BlockState): MutableText {
        val id = Registries.BLOCK.getId(state.block)

        return Text.literal(pos.toShortString()).formatted(YELLOW)
            .append(Text.literal(" → ").formatted(AQUA))
            .append(Text.literal(id.toString()).formatted(YELLOW))
    }

    override fun init(hooks: HookRegistrar) {
        args.translations.translateText(
            "type.${data.id()}.init",
            keybind("sneak", "use").formatted(YELLOW),
            keybind("sneak", "attack").formatted(YELLOW),
            keybind("swapOffhand").formatted(YELLOW)
        ).formatted(AQUA).sendTo(args.player())

        hooks.registerHook(PlayerInteractionHooks.ATTACK_BLOCK, AttackBlockCallback { entity, world, _, pos, _ ->
            attackBlock(entity, world, pos, args)
        })

        hooks.registerHook(PlayerInteractionHooks.USE_BLOCK, UseBlockCallback { entity, world, hand, result ->
            useBlock(entity, world, hand, result, args)
        })
    }

    private fun useBlock(
        entity: PlayerEntity,
        world: World,
        hand: Hand,
        result: BlockHitResult,
        args: SessionArgs
    ): ActionResult {
        if (entity != args.player() || world != args.world || !entity.isSneaking || hand != Hand.MAIN_HAND) return ActionResult.PASS

        val state = world.getBlockState(result.blockPos)
        blocks[result.blockPos] = state

        val marker = visualizer.markBlock(result.blockPos, state, DyeColor.LIGHT_BLUE.entityColor)
        val prev = markers.put(result.blockPos, marker)

        if (prev != null) {
            visualizer.removeEntity(prev)
        }

        args.translations.translateText(
            "type.positioned_block_set.added",
            label(result.blockPos, state)
        ).formatted(GREEN).sendTo(args.player())

        return ActionResult.FAIL
    }

    fun attackBlock(
        entity: PlayerEntity,
        world: World,
        pos: BlockPos,
        args: SessionArgs
    ): ActionResult {
        if (entity != args.player() || world != args.world || !entity.isSneaking) return ActionResult.PASS

        val state = blocks.remove(pos) ?: return ActionResult.FAIL

        val entity = markers.remove(pos)

        if (entity != null) visualizer.removeEntity(entity)

        args.translations.translateText(
            "type.positioned_block_set.removed",
            label(pos, state)
        ).formatted(RED).sendTo(args.player())

        return ActionResult.FAIL
    }

    override fun create() = PositionedBlockSet(blocks)
}