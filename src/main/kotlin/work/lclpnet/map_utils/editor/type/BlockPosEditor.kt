package work.lclpnet.map_utils.editor.type

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
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.map_api.data.type.BlockPosData
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.keybind

class BlockPosEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = BlockPosData.id(),
    override var propertyId: String? = null,
    override var role: String? = null,
    override var prevPropertyId: String? = null,
) : BaseDataEditor<BlockPos>(BlockPosData) {

    var pos: BlockPos? = null
    var marker: DisplayEntity.BlockDisplayEntity? = null

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<DialogInput>,
        translations: Translations,
        player: ServerPlayerEntity
    ) {
        body.add(required(pos, "pos", translations, player) { Text.literal(it.toShortString()) })
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
        this.pos = pos

        updateMarker()

        translations.translateText(
            key("set_pos"),
            styled(pos.toShortString(), Formatting.YELLOW)
        ).formatted(Formatting.GREEN).sendTo(player)

        return ActionResult.FAIL
    }

    override fun load(value: BlockPos) {
        this.pos = value

        updateMarker()
    }

    private fun updateMarker() {
        val prev = marker

        if (prev != null) {
            visualizer.removeEntity(prev)
        }

        val pos = this.pos ?: return

        marker = visualizer.markBlock(pos, DyeColor.LIME.entityColor)

    }

    override fun create(nbt: NbtCompound) = pos
}