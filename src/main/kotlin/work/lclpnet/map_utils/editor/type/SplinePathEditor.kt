package work.lclpnet.map_utils.editor.type

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.block.Blocks
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.dialog.body.DialogBody
import net.minecraft.dialog.type.DialogInput
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting.*
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d
import work.lclpnet.gaco.math.SplinePath
import work.lclpnet.gaco.scene.Object3d
import work.lclpnet.gaco.scene.`object`.BlockDisplayObject
import work.lclpnet.kibu.hook.HookRegistrar
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper.styled
import work.lclpnet.map_utils.LOGGER
import work.lclpnet.map_utils.data.type.SplinePathData
import work.lclpnet.map_utils.editor.BaseDataEditor
import work.lclpnet.map_utils.editor.SessionArgs
import work.lclpnet.map_utils.hook.VirtualEntityInteractCallback
import work.lclpnet.map_utils.util.Removable
import work.lclpnet.map_utils.util.keybind
import work.lclpnet.map_utils.util.toLocalizedShortString
import work.lclpnet.map_utils.visual.Visualizer
import work.lclpnet.map_utils.visual.displaySplinePath

private const val SELECTED_COLOR = 0x1cf411
private const val KEYPOINT_DATA_ID = "gmu:spline_path/keypoint"

class SplinePathEditor(
    override val args: SessionArgs,
    override val visualizer: Visualizer,
    override val id: String = SplinePathData.id(),
    override var propertyId: String? = null,
    override var prevPropertyId: String? = null
) : BaseDataEditor<SplinePath>(SplinePathData) {

    val keypoints = mutableListOf<Vec3d>()
    val interactions = mutableMapOf<Int, InteractionEntity>()
    var pathDisplay: Removable? = null
    var selectedIndex = -1

    override fun modifyDialog(
        body: MutableList<DialogBody>,
        inputs: MutableList<DialogInput>,
        translations: Translations,
        player: ServerPlayerEntity
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
            keybind("sprint", "swapOffhand").formatted(YELLOW),
            keybind("sprint", "use").formatted(YELLOW),
            keybind("sprint", "attack").formatted(YELLOW),
            keybind("swapOffhand").formatted(YELLOW)
        ).formatted(AQUA).sendTo(player)
    }

    override fun init(hooks: HookRegistrar) {
        hooks.registerHook(PlayerInventoryHooks.SWAP_HANDS, PlayerInventoryHooks.SwapHands { player, _ ->
            onSwapHands(player)
        })

        hooks.registerHook(VirtualEntityInteractCallback.HOOK, VirtualEntityInteractCallback { player, entityId ->
            provideKeypointInteractionHandler(player, entityId)
        })
    }

    private fun provideKeypointInteractionHandler(
        player: ServerPlayerEntity,
        entityId: Int
    ): Handler? {
        if (player != this.player || player.world != args.world) return null

        val interaction = interactions[entityId] ?: return null

        val data = interaction.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)

        if (!data.contains(KEYPOINT_DATA_ID)) return null

        val keypointData = data.get(KeypointData.MAP_CODEC)
            .resultOrPartial { LOGGER.error("Failed to decode keypoint data") }
            .orElse(null) ?: return null

        return object : Handler {
            override fun interact(hand: Hand) {
                selectKeypoint(keypointData.index)
            }

            override fun interactAt(hand: Hand, pos: Vec3d) {}

            override fun attack() {
                deleteKeypoint(keypointData.index)
            }
        }
    }

    private fun onSwapHands(player: ServerPlayerEntity): Boolean {
        if (player != this.player || player.world != world || !player.playerInput.sprint) return false

        addKeypoint(player.pos)

        translations.translateText(
            key("keypoint_added"),
            styled(keypoints.size, YELLOW),
            styled(player.pos.toLocalizedShortString(), YELLOW)
        ).formatted(GREEN).sendTo(player)

        return true
    }

    private fun addKeypoint(pos: Vec3d) {
        selectedIndex++
        keypoints.add(selectedIndex, pos)

        updateDisplay()
        return
    }

    private fun selectKeypoint(index: Int) {
        if (index < 0 || index >= keypoints.size || !player.playerInput.sprint) return

        selectedIndex = index
        updateDisplay()

        translations.translateText(
            key("keypoint_selected"),
            styled(index + 1, YELLOW),
        ).formatted(AQUA).sendTo(player)
    }

    private fun deleteKeypoint(index: Int) {
        if (index < 0 || index >= keypoints.size || !player.playerInput.sprint) return

        keypoints.removeAt(index)

        if (selectedIndex >= index) selectedIndex--

        updateDisplay()

        translations.translateText(
            key("keypoint_removed"),
            styled(index + 1, YELLOW),
            styled(player.pos.toLocalizedShortString(), YELLOW)
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
                val interaction = InteractionEntity(EntityType.INTERACTION, world)
                interaction.setPos(marker.position.x, marker.position.y - 0.25, marker.position.z)
                interaction.setResponse(true)
                interaction.interactionHeight = 0.5f
                interaction.interactionWidth = 0.5f

                NbtComponent.DEFAULT
                    .with(NbtOps.INSTANCE, KeypointData.MAP_CODEC, KeypointData(index))
                    .resultOrPartial { err -> LOGGER.error("Failed to encode keypoint data: $err") }
                    .ifPresent {
                        interaction.setComponent(DataComponentTypes.CUSTOM_DATA, it)
                    }

                interactions[interaction.id] = interaction

                visualizer.addEntity(interaction)

                labels.add(visualizer.text(marker.position.x, marker.position.y + 0.35, marker.position.z, Text.literal("#${index + 1}"), 0.5))
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
            markers.add(visualizer.marker(keypoint, Blocks.ORANGE_CONCRETE.defaultState, glowColor, 0.5))
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

    override fun create(nbt: NbtCompound): SplinePath? {
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