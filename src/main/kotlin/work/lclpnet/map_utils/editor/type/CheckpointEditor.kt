package work.lclpnet.map_utils.editor.type

import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.type.DialogInput
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Formatting.*
import net.minecraft.util.math.Vec3d
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
        inputs: MutableList<DialogInput>,
        translations: Translations,
        player: ServerPlayerEntity
    ) {
        respawnPosEditor.modifyDialog(body, inputs, translations, player)
        boundsEditor.modifyDialog(body, inputs, translations, player)
    }

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            translations.translateText(key("pos"))
                .translateFor(player),
            keybind("sprint", "swapOffhand").formatted(YELLOW),
            translations.translateText(key("pos1"))
                .formatted(BLUE)
                .translateFor(player),
            keybind("sprint", "attack").formatted(YELLOW),
            translations.translateText(key("pos2"))
                .formatted(RED)
                .translateFor(player),
            keybind("sprint", "use").formatted(YELLOW),
            keybind("swapOffhand").formatted(YELLOW)
        ).formatted(AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        respawnPosEditor.init(hooks)
        boundsEditor.init(hooks)
    }

    override fun load(value: Checkpoint) {
        respawnPosEditor.load(PositionRotation(value.pos.x, value.pos.y, value.pos.z, value.yaw, value.pitch))
        boundsEditor.load(value.bounds)
    }

    override fun create(nbt: NbtCompound): Checkpoint? {
        val posRot = respawnPosEditor.create(nbt)
        val bounds = boundsEditor.create(nbt)

        if (posRot == null || bounds == null) return null

        return Checkpoint(Vec3d(posRot.x, posRot.y, posRot.z), posRot.yaw, posRot.pitch, bounds)
    }
}