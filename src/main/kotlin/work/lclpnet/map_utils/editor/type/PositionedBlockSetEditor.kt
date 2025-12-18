package work.lclpnet.map_utils.editor.type

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.ChatFormatting.*
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.dialog.Input
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.dialog.input.BooleanInput
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
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
    val markers = mutableMapOf<BlockPos, Display.BlockDisplay>()
    var blocksPlaced = false

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<Input>,
        translations: Translations,
        player: ServerPlayer
    ) {
        body.add(messageBody(translations.translateText(player, key("header"), blocks.size)))

        for ((pos, state) in blocks) {
            body.add(messageBody(label(pos, state)))
        }

        inputs.add(
            Input(
                "placeBlocks",
                BooleanInput(
                    translations.translateText(key("place_blocks")).translateFor(player),
                    blocksPlaced,
                    "true",
                    "false"
                )
            )
        )
    }

    private fun label(pos: BlockPos, state: BlockState): MutableComponent {
        val id = BuiltInRegistries.BLOCK.getKey(state.block)

        return Component.literal(pos.toShortString()).withStyle(YELLOW)
            .append(Component.literal(" → ").withStyle(AQUA))
            .append(Component.literal(id.toString()).withStyle(YELLOW))
    }

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            keybind("sprint", "use").withStyle(YELLOW),
            keybind("sprint", "attack").withStyle(YELLOW),
            keybind("swapOffhand").withStyle(YELLOW)
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

    private fun blockBroken(world: Level, pos: BlockPos) {
        if (world != this.world) return

        removeBlock(pos)
    }

    private fun useBlock(
        entity: Player,
        world: Level,
        hand: InteractionHand,
        result: BlockHitResult
    ): InteractionResult {
        if (entity != player || world != this.world || !player.lastClientInput.sprint || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS

        val pos = result.blockPos
        val state = world.getBlockState(pos)
        blocks[pos] = state

        val marker = visualizer.markBlock(pos, state, DyeColor.LIGHT_BLUE.textureDiffuseColor)
        val prev = markers.put(pos, marker)

        if (prev != null) {
            visualizer.removeEntity(prev)
        }

        translations.translateText(
            key("added"),
            label(pos, state)
        ).formatted(GREEN).sendTo(player)

        return InteractionResult.FAIL
    }

    fun attackBlock(
        entity: Player,
        world: Level,
        pos: BlockPos
    ): InteractionResult {
        if (entity != player || world != this.world || !player.lastClientInput.sprint) return InteractionResult.PASS

        removeBlock(pos)

        return InteractionResult.FAIL
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

    override fun create(nbt: CompoundTag): PositionedBlockSet = PositionedBlockSet(blocks)

    override fun onDataChanged(nbt: CompoundTag) {
        nbt.getBoolean("placeBlocks").ifPresent {
            blocksPlaced = it
        }
    }

    override fun onTerminate(nbt: CompoundTag) {
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
            markers[pos] = visualizer.markBlock(pos, state, DyeColor.LIGHT_BLUE.textureDiffuseColor)
        }

        placeBlocks()
    }

    private fun placeBlocks() {
        val world = world

        for ((pos, state) in blocks) {
            world.setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE or Block.UPDATE_CLIENTS)
        }
    }

    private fun removeBlocks() {
        val world = world

        for ((pos, _) in blocks) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE or Block.UPDATE_CLIENTS or Block.UPDATE_SUPPRESS_DROPS)
        }
    }
}