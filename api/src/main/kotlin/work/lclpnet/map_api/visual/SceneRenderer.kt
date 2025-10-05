package work.lclpnet.map_api.visual

import net.minecraft.block.BlockState
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
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

    fun marker(pos: Vec3d, state: BlockState, glowColor: Int): Object3d {
        return marker(pos.x, pos.y, pos.z, state, glowColor)
    }

    fun marker(pos: Vec3d, state: BlockState, glowColor: Int, scale: Double): Object3d {
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

    fun text(pos: Vec3d, text: Text?): TextDisplayObject {
        return text(pos, text, 0.25)
    }

    fun text(x: Double, y: Double, z: Double, text: Text?): TextDisplayObject {
        return text(x, y, z, text, 0.25)
    }

    fun text(pos: Vec3d, text: Text?, scale: Double): TextDisplayObject {
        return text(pos.getX(), pos.getY(), pos.getZ(), text, scale)
    }

    fun text(x: Double, y: Double, z: Double, text: Text?, scale: Double): TextDisplayObject {
        val display = TextDisplayObject(scene, text)

        display.position.set(x, y, z)
        display.scale.set(scale)
        display.setBillboardMode(DisplayEntity.BillboardMode.CENTER)
        display.setBackground(0)

        display(display)

        return display
    }

    fun line(start: Vec3d, end: Vec3d, thickness: Double, state: BlockState): Object3d {
        return line(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), thickness, state)
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