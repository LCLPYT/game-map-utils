package work.lclpnet.map_utils.util

import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.Brightness
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting.AQUA
import net.minecraft.util.Formatting.YELLOW
import net.minecraft.util.math.Vec3d
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.map_utils.data.Data

fun createDataLabelDisplay(
    visualizer: Visualizer,
    player: ServerPlayerEntity,
    translations: Translations,
    propertyId: String,
    data: Data<*>,
    pos: Vec3d
): DisplayEntity.TextDisplayEntity {
    val textDisplay = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, visualizer.world())
    textDisplay.setBrightness(Brightness(15, 15))

    textDisplay.text = Text.empty()
        .append(translations.translateText("type.${data.id()}").formatted(AQUA).translateFor(player))
        .append(Text.literal("\n\"$propertyId\"").formatted(YELLOW))

    textDisplay.billboardMode = DisplayEntity.BillboardMode.CENTER
    textDisplay.isGlowing = true

    textDisplay.setPosition(pos)

    visualizer.addEntity(textDisplay)

    return textDisplay
}