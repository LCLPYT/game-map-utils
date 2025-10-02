package work.lclpnet.map_utils.editor.type

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.dialog.body.DialogBody
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.DyeColor
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.AffineTransformation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.joml.Matrix4f
import work.lclpnet.gaco.ds.BlockBox
import work.lclpnet.gaco.dynamic_entities.PlayerSpecificDynamicEntity
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.map_utils.data.DataEditor
import work.lclpnet.map_utils.data.type.BlockBoxData
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.keybind

class BlockBoxEditor(val data: BlockBoxData, val propertyId: String?) : DataEditor<BlockBox> {
    override fun data() = data
    override fun propertyId() = propertyId

    var pos1: BlockPos? = null
    var pos2: BlockPos? = null

    var pos1Marker: DisplayEntity.BlockDisplayEntity? = null
    var pos2Marker: DisplayEntity.BlockDisplayEntity? = null
    var boxMarker: DisplayEntity.BlockDisplayEntity? = null

    override fun addBody(body: MutableList<DialogBody>, translations: Translations, player: ServerPlayerEntity) {
        body.add(required(pos1, "pos1", translations, player) { Text.literal(it.toShortString()) })
        body.add(required(pos2, "pos2", translations, player) { Text.literal(it.toShortString()) })
    }

    override fun init(hooks: HookRegistrar, args: SessionArgs) {
        args.translations.translateText(
            "type.${data.id()}.init",
            keybind("sneak", "attack").formatted(Formatting.YELLOW),
            keybind("sneak", "use").formatted(Formatting.YELLOW),
            keybind("swapOffhand").formatted(Formatting.YELLOW)
        ).formatted(Formatting.AQUA).sendTo(args.player())

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

        pos2 = result.blockPos.toImmutable()
        sendPosChanged(args, "type.block_box.set_pos2", result.blockPos)
        pos2Marker = markPosition(pos2Marker, result.blockPos, args, Blocks.RED_CONCRETE, DyeColor.RED.entityColor)

        updateBox(args)

        return ActionResult.FAIL
    }

    fun attackBlock(
        entity: PlayerEntity,
        world: World,
        pos: BlockPos,
        args: SessionArgs
    ): ActionResult {
        if (entity != args.player() || world != args.world || !entity.isSneaking) return ActionResult.PASS

        pos1 = pos.toImmutable()
        sendPosChanged(args, "type.block_box.set_pos1", pos)
        pos1Marker = markPosition(pos1Marker, pos, args, Blocks.BLUE_CONCRETE, DyeColor.BLUE.entityColor)

        updateBox(args)

        return ActionResult.FAIL
    }

    private fun sendPosChanged(args: SessionArgs, key: String, pos: BlockPos) {
        args.translations.translateText(
            key,
            styled(pos.toShortString(), Formatting.YELLOW)
        ).formatted(Formatting.GREEN).sendTo(args.player())
    }

    private fun markPosition(marker: DisplayEntity.BlockDisplayEntity?, pos: BlockPos, args: SessionArgs, block: Block, color: Int): DisplayEntity.BlockDisplayEntity {
        if (marker != null) {
            marker.setPosition(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            return marker
        }

        val margin = 0.015f
        val marker = DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, args.world)
        marker.setPosition(
            pos.x.toDouble() + margin,
            pos.y.toDouble() + margin,
            pos.z.toDouble() + margin
        )
        marker.setTransformation(AffineTransformation(Matrix4f().scale(1f - 2 * margin)))
        marker.blockState = block.defaultState
        marker.isGlowing = true
        marker.glowColorOverride = color

        args.dynamicEntityManager.add(PlayerSpecificDynamicEntity(marker, args.player().uuid))

        return marker
    }

    private fun updateBox(args: SessionArgs) {
        val pos1 = pos1
        val pos2 = pos2

        if (pos1 == null || pos2 == null) return

        val box = BlockBox(pos1, pos2)

        var marker = boxMarker

        if (marker != null) {
            marker.setPos(box.min().x.toDouble(), box.min().y.toDouble(), box.min().z.toDouble())
            marker.setTransformation(
                AffineTransformation(
                    Matrix4f()
                        .scale(box.width().toFloat(), box.height().toFloat(), box.length().toFloat())
                )
            )
            return
        }

        marker = DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, args.world)
        marker.blockState = Blocks.GREEN_STAINED_GLASS.defaultState
        marker.setPos(box.min().x.toDouble(), box.min().y.toDouble(), box.min().z.toDouble())
        marker.setTransformation(
            AffineTransformation(
                Matrix4f()
                    .scale(box.width().toFloat(), box.height().toFloat(), box.length().toFloat())
            )
        )

        boxMarker = marker

        args.dynamicEntityManager.add(PlayerSpecificDynamicEntity(marker, args.player().uuid))
    }

    override fun create(input: NbtCompound): BlockBox? {
        val pos1 = this.pos1
        val pos2 = this.pos2

        if (pos1 == null) {

        }

        if (pos1 == null || pos2 == null) return null

        return BlockBox(pos1, pos2)
    }
}
