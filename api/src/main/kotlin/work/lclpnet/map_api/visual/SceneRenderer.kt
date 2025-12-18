package work.lclpnet.map_api.visual

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Display
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import work.lclpnet.gaco.scene.Object3d
import work.lclpnet.gaco.scene.Scene
import work.lclpnet.gaco.scene.`object`.BlockDisplayObject
import work.lclpnet.gaco.scene.`object`.TextDisplayObject
import kotlin.math.sqrt

interface SceneRenderer {

    val scene: Scene

    fun display(obj: Object3d) {
        scene.add(obj)
    }

    fun marker(pos: Vec3, state: BlockState, glowColor: Int): Object3d {
        return marker(pos.x, pos.y, pos.z, state, glowColor)
    }

    fun marker(pos: Vec3, state: BlockState, glowColor: Int, scale: Double): Object3d {
        return marker(pos.x, pos.y, pos.z, state, glowColor, scale)
    }

    fun marker(x: Double, y: Double, z: Double, state: BlockState, glowColor: Int): Object3d {
        return marker(x, y, z, state, glowColor, 0.25)
    }

    fun marker(x: Double, y: Double, z: Double, state: BlockState, glowColor: Int, scale: Double): Object3d {
        val marker = BlockDisplayObject(scene, state)

        marker.position.set(-0.5, -0.5, -0.5)
        marker.setGlowing(true)
        marker.setGlowColorOverride(glowColor)
        marker.setInterpolationDuration(1)

        val wrapper = Object3d(scene)
        wrapper.position.set(x, y, z)
        wrapper.scale.set(scale)
        wrapper.addChild(marker)

        display(wrapper)

        return wrapper
    }

    fun text(pos: Vec3, text: Component?): TextDisplayObject {
        return text(pos, text, 0.25)
    }

    fun text(x: Double, y: Double, z: Double, text: Component?): TextDisplayObject {
        return text(x, y, z, text, 0.25)
    }

    fun text(pos: Vec3, text: Component?, scale: Double): TextDisplayObject {
        return text(pos.x(), pos.y(), pos.z(), text, scale)
    }

    fun text(x: Double, y: Double, z: Double, text: Component?, scale: Double): TextDisplayObject {
        val display = TextDisplayObject(scene, text)

        display.position.set(x, y, z)
        display.scale.set(scale)
        display.setBillboardMode(Display.BillboardConstraints.CENTER)
        display.setBackground(0)

        display(display)

        return display
    }

    fun line(start: Vec3, end: Vec3, thickness: Double, state: BlockState): Object3d {
        return line(start.x(), start.y(), start.z(), end.x(), end.y(), end.z(), thickness, state)
    }

    fun line(
        x1: Double,
        y1: Double,
        z1: Double,
        x2: Double,
        y2: Double,
        z2: Double,
        thickness: Double,
        state: BlockState
    ): Object3d {
        val base = BlockDisplayObject(scene, state)

        // base will be pointing in positive x direction, center in y and z plane
        base.position.set(0.0, -0.5, -0.5)

        val line = Object3d(scene)
        line.addChild(base)

        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1

        line.position.set(x1, y1, z1)

        val len = sqrt(dx * dx + dy * dy + dz * dz)

        // line points in x-direction
        line.scale.set(len, thickness, thickness)

        line.rotation.rotateTo(1.0, 0.0, 0.0, dx / len, dy / len, dz / len)

        display(line)

        return line
    }

    fun destroy() {
        scene.clear()
    }
}