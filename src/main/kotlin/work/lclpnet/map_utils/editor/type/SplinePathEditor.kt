package work.lclpnet.map_utils.editor.type

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.ChatFormatting.*
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.Input
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Interaction
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import work.lclpnet.gaco.math.SplinePath
import work.lclpnet.gaco.scene.Object3d
import work.lclpnet.gaco.scene.`object`.BlockDisplayObject
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.map_api.LOGGER
import work.lclpnet.map_api.data.type.SplinePathData
import work.lclpnet.map_api.visual.Removable
import work.lclpnet.map_api.visual.Visualizer
import work.lclpnet.map_api.visual.displaySplinePath
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.hook.VirtualEntityAttackCallback
import work.lclpnet.map_utils.hook.VirtualEntityInteractCallback
import work.lclpnet.map_utils.util.keybind
import work.lclpnet.map_utils.util.toLocalizedShortString

private const val SELECTED_COLOR = 0x1cf411
private const val KEYPOINT_DATA_ID = "gmu:spline_path/keypoint"

class SplinePathEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = SplinePathData.id(),
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null,
    override var role: String? = null,
) : BaseDataEditor<SplinePath>(SplinePathData) {

    val keypoints = mutableListOf<Vec3>()
    val interactions = mutableMapOf<Int, Interaction>()
    var pathDisplay: Removable? = null
    var selectedIndex = -1

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<Input>,
        translations: Translations,
        player: ServerPlayer
    ) {
        body.add(messageBody(translations.translateText(player, key("header"), keypoints.size)))

        if (keypoints.size < 2) {
            body.add(messageBody(translations.translateText(key("too_few_keypoints"))
                .formatted(RED)
                .translateFor(player)))
        }
    }

    override fun sendTutorial() {
        translations.translateText(
            key("init"),
            keybind("sprint", "swapOffhand").withStyle(YELLOW),
            keybind("sprint", "use").withStyle(YELLOW),
            keybind("sprint", "attack").withStyle(YELLOW),
            keybind("swapOffhand").withStyle(YELLOW)
        ).formatted(AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInventoryHooks.SWAP_HANDS, PlayerInventoryHooks.SwapHands { player, _ ->
            onSwapHands(player)
        })

        hooks.registerHook(VirtualEntityInteractCallback.HOOK, VirtualEntityInteractCallback { player, entityId, _, _ ->
            provideKeypointInteractionHandler(player, entityId) { selectKeypoint(it) }
        })

        hooks.registerHook(VirtualEntityAttackCallback.HOOK, VirtualEntityAttackCallback { player, entityId ->
            provideKeypointInteractionHandler(player, entityId) { deleteKeypoint(it) }
        })
    }

    private fun provideKeypointInteractionHandler(
        player: ServerPlayer,
        entityId: Int,
        action: (Int) -> Unit,
    ) {
        if (player != this.player || player.level() != args.world) return

        val interaction = interactions[entityId] ?: return

        val data = interaction.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)

        val nbt = data.copyTag()

        if (!nbt.contains(KEYPOINT_DATA_ID)) return

        val keypointData = KeypointData.MAP_CODEC.codec().decode(NbtOps.INSTANCE, nbt)
            .resultOrPartial { LOGGER.error("Failed to decode keypoint data") }
            .map { it.first }
            .orElse(null) ?: return

        action(keypointData.index)
    }

    private fun onSwapHands(player: ServerPlayer): Boolean {
        if (player != this.player || player.level() != world || !player.lastClientInput.sprint) return false

        addKeypoint(player.position())

        translations.translateText(
            key("keypoint_added"),
            styled(keypoints.size, YELLOW),
            styled(player.position().toLocalizedShortString(), YELLOW)
        ).formatted(GREEN).sendTo(player)

        return true
    }

    private fun addKeypoint(pos: Vec3) {
        selectedIndex++
        keypoints.add(selectedIndex, pos)

        updateDisplay()
        return
    }

    private fun selectKeypoint(index: Int) {
        if (index < 0 || index >= keypoints.size || !player.lastClientInput.sprint) return

        selectedIndex = index
        updateDisplay()

        translations.translateText(
            key("keypoint_selected"),
            styled(index + 1, YELLOW),
        ).formatted(AQUA).sendTo(player)
    }

    private fun deleteKeypoint(index: Int) {
        if (index < 0 || index >= keypoints.size || !player.lastClientInput.sprint) return

        keypoints.removeAt(index)

        if (selectedIndex >= index) selectedIndex--

        updateDisplay()

        translations.translateText(
            key("keypoint_removed"),
            styled(index + 1, YELLOW),
            styled(player.position().toLocalizedShortString(), YELLOW)
        ).formatted(RED).sendTo(player)
    }

    private fun updateDisplay() {
        pathDisplay?.remove()
        interactions.values.forEach { visualizer.removeEntity(it) }
        interactions.clear()

        if (keypoints.size >= 2) {
            val path = SplinePath.create(keypoints, LOGGER).orElse(null) ?: return

            val (display, keypointMarkers) = displaySplinePath(path, visualizer)

            keypointMarkers[selectedIndex].traverse().forEach {
                if (it is BlockDisplayObject) {
                    it.glowColorOverride = SELECTED_COLOR
                }
            }

            val labels = mutableListOf<Object3d>()

            keypointMarkers.withIndex().forEach { (index, marker) ->
                val interaction = Interaction(EntityType.INTERACTION, world)
                interaction.setPosRaw(marker.position.x, marker.position.y - 0.25, marker.position.z)
                interaction.setResponse(true)
                interaction.height = 0.5f
                interaction.width = 0.5f

                KeypointData.MAP_CODEC.codec().encode(KeypointData(index), NbtOps.INSTANCE, CompoundTag())
                    .resultOrPartial { err -> LOGGER.error("Failed to encode keypoint data: $err") }
                    .ifPresent {
                        interaction.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(it as CompoundTag))
                    }

                interactions[interaction.id] = interaction

                visualizer.addEntity(interaction)

                labels.add(visualizer.text(marker.position.x, marker.position.y + 0.35, marker.position.z, Component.literal("#${index + 1}"), 0.5))
            }

            pathDisplay = Removable {
                display.remove()
                labels.forEach { it.detach() }
                labels.clear()
            }

            return
        }

        val markers = mutableListOf<Object3d>()

        for ((i, keypoint) in keypoints.withIndex()) {
            val glowColor = if (i == selectedIndex) SELECTED_COLOR else 0xeeff00
            markers.add(visualizer.marker(keypoint, Blocks.ORANGE_CONCRETE.defaultBlockState(), glowColor, 0.5))
        }

        pathDisplay = Removable {
            markers.forEach { it.detach() }
            markers.clear()

            interactions.values.forEach { visualizer.removeEntity(it) }
            interactions.clear()
        }
    }

    override fun load(value: SplinePath) {
        selectedIndex = -1
        keypoints.clear()

        keypoints.addAll(value.keypoints)

        selectedIndex = keypoints.size - 1

        updateDisplay()
    }

    override fun create(nbt: CompoundTag): SplinePath? {
        if (keypoints.size < 2) {
            translations.translateText(key("too_few_keypoints"))
                .formatted(RED)
                .sendTo(player)
            return null
        }
        
        return SplinePath.create(keypoints, LOGGER).orElse(null)
    }
}

private data class KeypointData(val index: Int) {
    companion object {
        val CODEC: Codec<KeypointData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("index").forGetter { it.index }
            ).apply(instance) { index ->
                KeypointData(index)
            }
        }

        val MAP_CODEC: MapCodec<KeypointData> = CODEC.fieldOf(KEYPOINT_DATA_ID)
    }
}