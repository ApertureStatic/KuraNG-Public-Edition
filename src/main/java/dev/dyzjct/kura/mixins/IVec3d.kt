package dev.dyzjct.kura.mixins

import net.minecraft.util.math.Vec3i
import org.joml.Vector3d

interface IVec3d {
    operator fun set(x: Double, y: Double, z: Double)
    fun set(vec: Vec3i) {
        set(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
    }

    fun set(vec: Vector3d) {
        set(vec.x, vec.y, vec.z)
    }

    fun setXZ(x: Double, z: Double)
    fun setY(y: Double)
}
