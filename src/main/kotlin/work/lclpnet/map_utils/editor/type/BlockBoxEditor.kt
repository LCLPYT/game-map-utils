package work.lclpnet.map_utils.editor.type

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.type.DialogInput
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.DyeColor
import net.minecraft.util.Formatting
import net.minecraft.util.Formatting.BLUE
import net.minecraft.util.Formatting.RED
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import work.lclpnet.gaco.ds.BlockBox
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.map_utils.data.type.BlockBoxData
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.util.Visualizer
import work.lclpnet.map_utils.util.keybind

class BlockBoxEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = BlockBoxData.id(),
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null,
) : BaseDataEditor<BlockBox>(BlockBoxData) {

    var pos1: BlockPos? = null
    var pos2: BlockPos? = null

    var pos1Marker: DisplayEntity.BlockDisplayEntity? = null
    var pos2Marker: DisplayEntity.BlockDisplayEntity? = null
    var boxMarker: Removable? = null

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<DialogInput>,
        translations: Translations,
        player: ServerPlayerEntity
    ) {
        body.add(required(pos1, "pos1", translations, player) { Text.literal(it.toShortString()) })
        body.add(required(pos2, "pos2", translations, player) { Text.literal(it.toShortString()) })
    }

    override fun sendTutorial() {
        args.translations.translateText(
            key("init"),
            args.translations.translateText(key("pos1"))
                .formatted(BLUE)
                .translateFor(args.player()),
            keybind("sprint", "attack").formatted(Formatting.YELLOW),
            args.translations.translateText(key("pos2"))
                .formatted(RED)
                .translateFor(args.player()),
            keybind("sprint", "use").formatted(Formatting.YELLOW),
            keybind("swapOffhand").formatted(Formatting.YELLOW)
        ).formatted(Formatting.AQUA).sendTo(args.player())
    }

    override fun init(hooks: HookRegistrar) {
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

        pos2 = pos.toImmutable()
        sendPosChanged(args, key("set_pos2"), pos)

        pos2Marker = updatePosMarker(pos, world, pos2Marker, DyeColor.RED.entityColor)

        updateBox()

        return ActionResult.FAIL
    }

    fun attackBlock(
        entity: PlayerEntity,
        world: World,
        pos: BlockPos,
        args: SessionArgs
    ): ActionResult {
        if (entity != args.player() || world != args.world || !args.player().playerInput.sprint) return ActionResult.PASS

        pos1 = pos.toImmutable()
        sendPosChanged(args, key("set_pos1"), pos)

        pos1Marker = updatePosMarker(pos, world, pos1Marker, DyeColor.BLUE.entityColor)

        updateBox()

        return ActionResult.FAIL
    }

    private fun updatePosMarker(pos: BlockPos, world: World, marker: DisplayEntity.BlockDisplayEntity?, color: Int): DisplayEntity.BlockDisplayEntity {
        if (marker != null) {
            visualizer.removeEntity(marker)
        }

        return visualizer.markBlock(pos, world.getBlockState(pos), color)
    }

    private fun sendPosChanged(args: SessionArgs, key: String, pos: BlockPos) {
        args.translations.translateText(
            key,
            styled(pos.toShortString(), Formatting.YELLOW)
        ).formatted(Formatting.GREEN).sendTo(args.player())
    }

    private fun updateBox() {
        val pos1 = pos1
        val pos2 = pos2

        if (pos1 == null || pos2 == null) return

        val box = BlockBox(pos1, pos2)

        boxMarker?.remove()

        boxMarker = data.display(box, visualizer, args.player(), args.translations, id, propertyId)
    }

    override fun create(nbt: NbtCompound): BlockBox? {
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

        pos1Marker = updatePosMarker(value.min(), args.world, pos1Marker, DyeColor.BLUE.entityColor)
        pos2Marker = updatePosMarker(value.max(), args.world, pos2Marker, DyeColor.RED.entityColor)

        updateBox()
    }
}
