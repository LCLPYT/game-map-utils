package work.lclpnet.map_utils.editor.type

import net.minecraft.ChatFormatting.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.dialog.Input
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import work.lclpnet.gaco.ds.Checkpoint
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.util.PositionRotation
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_api.data.type.CheckpointData
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.util.keybind

class CheckpointEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = CheckpointData.id(),
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null,
    override var role: String? = null,
) : BaseDataEditor<Checkpoint>(CheckpointData) {

    val respawnPosEditor = PositionEditor(args, visualizer, id)
    val boundsEditor = BlockBoxEditor(args, visualizer, id)

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<Input>,
        translations: Translations,
        player: ServerPlayer
    ) {
        respawnPosEditor.modifyDialog(body, inputs, translations, player)
        boundsEditor.modifyDialog(body, inputs, translations, player)
    }

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            translations.translateText(key("pos"))
                .translateFor(player),
            keybind("sprint", "swapOffhand").withStyle(YELLOW),
            translations.translateText(key("pos1"))
                .withStyle(BLUE)
                .translateFor(player),
            keybind("sprint", "attack").withStyle(YELLOW),
            translations.translateText(key("pos2"))
                .withStyle(RED)
                .translateFor(player),
            keybind("sprint", "use").withStyle(YELLOW),
            keybind("swapOffhand").withStyle(YELLOW)
        ).withStyle(AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        respawnPosEditor.init(hooks)
        boundsEditor.init(hooks)
    }

    override fun load(value: Checkpoint) {
        respawnPosEditor.load(PositionRotation(value.pos.x, value.pos.y, value.pos.z, value.yaw, value.pitch))
        boundsEditor.load(value.bounds)
    }

    override fun create(nbt: CompoundTag): Checkpoint? {
        val posRot = respawnPosEditor.create(nbt)
        val bounds = boundsEditor.create(nbt)

        if (posRot == null || bounds == null) return null

        return Checkpoint(Vec3(posRot.x(), posRot.y(), posRot.z()), posRot.yaw, posRot.pitch, bounds)
    }
}