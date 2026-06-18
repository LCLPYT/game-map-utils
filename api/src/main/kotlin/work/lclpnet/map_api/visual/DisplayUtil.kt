package work.lclpnet.map_api.visual

import net.minecraft.ChatFormatting.AQUA
import net.minecraft.ChatFormatting.YELLOW
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.phys.Vec3
import work.lclpnet.kibu.translate.Translations

fun createDataLabelDisplay(
    visualizer: Visualizer,
    player: ServerPlayer,
    translations: Translations,
    propertyId: String,
    id: String,
    pos: Vec3
): Display.TextDisplay {
    val textDisplay = Display.TextDisplay(EntityTypes.TEXT_DISPLAY, visualizer.world())
    textDisplay.brightnessOverride = Brightness(15, 15)

    textDisplay.text = Component.empty()
        .append(translations.translateText("type.$id").withStyle(AQUA).translateFor(player))
        .append(Component.literal("\n\"$propertyId\"").withStyle(YELLOW))

    textDisplay.billboardConstraints = Display.BillboardConstraints.CENTER
    textDisplay.setGlowingTag(true)

    textDisplay.setPos(pos)

    visualizer.addEntity(textDisplay)

    return textDisplay
}