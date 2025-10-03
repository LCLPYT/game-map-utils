package work.lclpnet.map_utils.editor.type

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.input.BooleanInputControl
import net.minecraft.dialog.type.DialogInput
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
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
import work.lclpnet.map_utils.util.PositionedBlockSet
import work.lclpnet.map_utils.util.Visualizer
import work.lclpnet.map_utils.util.keybind

class PositionedBlockSetEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null
) : BaseDataEditor<PositionedBlockSet>(PositionedBlockSetData) {

    val blocks = mutableMapOf<BlockPos, BlockState>()
    val markers = mutableMapOf<BlockPos, DisplayEntity.BlockDisplayEntity>()
    var blocksPlaced = false

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<DialogInput>,
        translations: Translations,
        player: ServerPlayerEntity
    ) {
        body.add(messageBody(translations.translateText(player, key("header"), blocks.size)))

        for ((pos, state) in blocks) {
            body.add(messageBody(label(pos, state)))
        }

        inputs.add(
            DialogInput(
                "placeBlocks",
                BooleanInputControl(
                    translations.translateText(key("place_blocks")).translateFor(player),
                    blocksPlaced,
                    "true",
                    "false"
                )
            )
        )
    }

    private fun label(pos: BlockPos, state: BlockState): MutableText {
        val id = Registries.BLOCK.getId(state.block)

        return Text.literal(pos.toShortString()).formatted(YELLOW)
            .append(Text.literal(" → ").formatted(AQUA))
            .append(Text.literal(id.toString()).formatted(YELLOW))
    }

    override fun init(hooks: HookRegistrar) {
        args.translations.translateText(
            key("init"),
            keybind("sprint", "use").formatted(YELLOW),
            keybind("sprint", "attack").formatted(YELLOW),
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
        if (entity != args.player() || world != args.world || !args.player().playerInput.sprint || hand != Hand.MAIN_HAND) return ActionResult.PASS

        val pos = result.blockPos
        val state = world.getBlockState(pos)
        blocks[pos] = state

        val marker = visualizer.markBlock(pos, state, DyeColor.LIGHT_BLUE.entityColor)
        val prev = markers.put(pos, marker)

        if (prev != null) {
            visualizer.removeEntity(prev)
        }

        args.translations.translateText(
            key("added"),
            label(pos, state)
        ).formatted(GREEN).sendTo(args.player())

        return ActionResult.FAIL
    }

    fun attackBlock(
        entity: PlayerEntity,
        world: World,
        pos: BlockPos,
        args: SessionArgs
    ): ActionResult {
        if (entity != args.player() || world != args.world || !args.player().playerInput.sprint) return ActionResult.PASS

        val state = blocks.remove(pos) ?: return ActionResult.FAIL

        val entity = markers.remove(pos)

        if (entity != null) visualizer.removeEntity(entity)

        args.translations.translateText(
            key("removed"),
            label(pos, state)
        ).formatted(RED).sendTo(args.player())

        return ActionResult.FAIL
    }

    override fun create(nbt: NbtCompound): PositionedBlockSet = PositionedBlockSet(blocks)

    override fun onDataChanged(nbt: NbtCompound) {
        nbt.getBoolean("placeBlocks").ifPresent {
            blocksPlaced = it
        }
    }

    override fun onTerminate(nbt: NbtCompound) {
        nbt.getBoolean("placeBlocks").ifPresent {
            if (it) placeBlocks()
            else removeBlocks()
        }
    }

    override fun load(value: PositionedBlockSet) {
        removeBlocks()
        blocks.clear()

        for ((_, entity) in markers) {
            visualizer.removeEntity(entity)
        }

        markers.clear()

        for ((pos, state) in value) {
            blocks[pos] = state
            markers[pos] = visualizer.markBlock(pos, state, DyeColor.LIGHT_BLUE.entityColor)
        }

        placeBlocks()
    }

    private fun placeBlocks() {
        val world = args.world

        for ((pos, state) in blocks) {
            world.setBlockState(pos, state, Block.FORCE_STATE or Block.NOTIFY_LISTENERS)
        }
    }

    private fun removeBlocks() {
        val world = args.world

        for ((pos, _) in blocks) {
            world.setBlockState(pos, Blocks.AIR.defaultState, Block.FORCE_STATE or Block.NOTIFY_LISTENERS or Block.SKIP_DROPS)
        }
    }
}