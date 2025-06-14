package dev.dyzjct.kura.utils.rotation

import net.minecraft.util.math.Vec3d

data class Rotation(
    var yaw: Float,
    var pitch: Float,
    var priority: Int = 114514,
    var time: Long = System.currentTimeMillis()
) {

    companion object {
        val ZERO = Rotation(0f, 0f)
    }

    operator fun minus(prevRotation: Rotation): Rotation {
        return Rotation(yaw - prevRotation.yaw, pitch - prevRotation.pitch)
    }

    operator fun plus(prevRotation: Rotation): Rotation {
        return Rotation(yaw + prevRotation.yaw, pitch + prevRotation.pitch)
    }

    operator fun times(value: Float): Rotation {
        return Rotation(yaw * value, pitch * value)
    }

    operator fun div(value: Float): Rotation {
        return Rotation(yaw / value, pitch / value)
    }

}

data class VecRotation(val rotation: Rotation, val vec: Vec3d)
