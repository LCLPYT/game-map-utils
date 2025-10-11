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
import work.lclpnet.gaco.ds.PositionedBlockSet
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks
import work.lclpnet.kibu.hook.world.BlockModificationHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.type.PositionedBlockSetData
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.keybind

class PositionedBlockSetEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = PositionedBlockSetData.id(),
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null,
    override var role: String? = null,
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

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            keybind("sprint", "use").formatted(YELLOW),
            keybind("sprint", "attack").formatted(YELLOW),
            keybind("swapOffhand").formatted(YELLOW)
        ).formatted(AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInteractionHooks.ATTACK_BLOCK, AttackBlockCallback { entity, world, _, pos, _ ->
            attackBlock(entity, world, pos)
        })

        hooks.registerHook(PlayerInteractionHooks.USE_BLOCK, UseBlockCallback { entity, world, hand, result ->
            useBlock(entity, world, hand, result)
        })

        hooks.registerHook(BlockModificationHooks.BLOCK_BROKEN, BlockModificationHooks.BlockModifiedHook { world, pos, _ ->
            blockBroken(world, pos)
        })
    }

    private fun blockBroken(world: World, pos: BlockPos) {
        if (world != this.world) return

        removeBlock(pos)
    }

    private fun useBlock(
        entity: PlayerEntity,
        world: World,
        hand: Hand,
        result: BlockHitResult
    ): ActionResult {
        if (entity != player || world != this.world || !player.playerInput.sprint || hand != Hand.MAIN_HAND) return ActionResult.PASS

        val pos = result.blockPos
        val state = world.getBlockState(pos)
        blocks[pos] = state

        val marker = visualizer.markBlock(pos, state, DyeColor.LIGHT_BLUE.entityColor)
        val prev = markers.put(pos, marker)

        if (prev != null) {
            visualizer.removeEntity(prev)
        }

        translations.translateText(
            key("added"),
            label(pos, state)
        ).formatted(GREEN).sendTo(player)

        return ActionResult.FAIL
    }

    fun attackBlock(
        entity: PlayerEntity,
        world: World,
        pos: BlockPos
    ): ActionResult {
        if (entity != player || world != this.world || !player.playerInput.sprint) return ActionResult.PASS

        removeBlock(pos)

        return ActionResult.FAIL
    }

    fun removeBlock(pos: BlockPos) {
        val state = blocks.remove(pos) ?: return

        val entity = markers.remove(pos)

        if (entity != null) visualizer.removeEntity(entity)

        translations.translateText(
            key("removed"),
            label(pos, state)
        ).formatted(RED).sendTo(player)
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
        val world = world

        for ((pos, state) in blocks) {
            world.setBlockState(pos, state, Block.FORCE_STATE or Block.NOTIFY_LISTENERS)
        }
    }

    private fun removeBlocks() {
        val world = world

        for ((pos, _) in blocks) {
            world.setBlockState(pos, Blocks.AIR.defaultState, Block.FORCE_STATE or Block.NOTIFY_LISTENERS or Block.SKIP_DROPS)
        }
    }
}