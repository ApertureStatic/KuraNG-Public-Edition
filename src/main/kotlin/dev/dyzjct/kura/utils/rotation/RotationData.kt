/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.dyzjct.kura.utils.rotation

import dev.dyzjct.kura.utils.rotation.RotationUtils.gcd
import dev.dyzjct.kura.utils.rotation.RotationUtils.serverRotation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.roundToInt

data class Rotation(
    var yaw: Float,
    var pitch: Float
) {

    companion object {
        val ZERO = Rotation(0f, 0f)
    }

    val rotationVec: Vec3d
        get() {
            val yawCos = MathHelper.cos(-yaw * 0.017453292f)
            val yawSin = MathHelper.sin(-yaw * 0.017453292f)
            val pitchCos = MathHelper.cos(pitch * 0.017453292f)
            val pitchSin = MathHelper.sin(pitch * 0.017453292f)
            return Vec3d((yawSin * pitchCos).toDouble(), (-pitchSin).toDouble(), (yawCos * pitchCos).toDouble())
        }

    /**
     * Fix rotation based on sensitivity
     */
    fun fixedSensitivity(): Rotation {
        val gcd = gcd

        // get previous rotation
        val rotation = serverRotation

        // get rotation differences
        val (deltaYaw, deltaPitch) = Rotation(yaw - rotation.yaw, pitch - rotation.pitch)

        var g1 = 0f
        var g2 = 0f
        // proper rounding
        kotlin.runCatching {
            g1 = ((deltaYaw / gcd).roundToInt() * gcd).toFloat()
            g2 = ((deltaPitch / gcd).roundToInt() * gcd).toFloat()
        }

        // fix rotation
        val yaw = rotation.yaw + g1
        val pitch = rotation.pitch + g2

        return Rotation(yaw, pitch.coerceIn(-90f, 90f))
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
