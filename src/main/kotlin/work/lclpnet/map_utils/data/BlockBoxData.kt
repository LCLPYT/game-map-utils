package work.lclpnet.map_utils.data

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.dialog.body.DialogBody
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.keybind

class BlockBoxData() : Data {
    override fun id() = "block_box"
    override fun createEditor(propertyId: String?) = BlockBoxEditor(this, propertyId)
}

class BlockBoxEditor(val data: BlockBoxData, val propertyId: String?) : DataEditor {
    override fun data() = data
    override fun propertyId() = propertyId

    var pos1: BlockPos? = null
    var pos2: BlockPos? = null

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

        args.translations.translateText(
            "type.block_box.set_pos2",
            FormatWrapper.styled(result.blockPos.toShortString(), Formatting.YELLOW)
        ).formatted(Formatting.GREEN).sendTo(args.player())

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

        args.translations.translateText(
            "type.block_box.set_pos1",
            FormatWrapper.styled(pos.toShortString(), Formatting.YELLOW)
        ).formatted(Formatting.GREEN).sendTo(args.player())

        return ActionResult.FAIL
    }
}