package base.utils.player

import base.system.event.SafeClientEvent
import base.utils.Wrapper
import dev.dyzjct.kura.utils.math.RotationUtils.getRotationTo
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.*

object RotationUtils {

    fun SafeClientEvent.getPlayerDirection(): Direction {
        val directionNum = MathHelper.floor((player.renderYaw * 4.0f / 360.0f).toDouble() + 0.5) and 3
        if (directionNum == 0) {
            return Direction.SOUTH
        }
        if (directionNum == 1) {
            return Direction.WEST
        }
        if (directionNum == 2) {
            return Direction.NORTH
        }
        return Direction.EAST
    }

    fun SafeClientEvent.directionSpeed(speed: Double): DoubleArray {
        var forward: Float = player.input.movementForward
        var side: Float = player.input.movementSideways
        var yaw: Float =
            player.prevYaw + (player.yaw - player.prevYaw) * mc.tickDelta
        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += (if (forward > 0.0f) -45 else 45).toFloat()
            } else if (side < 0.0f) {
                yaw += (if (forward > 0.0f) 45 else -45).toFloat()
            }
            side = 0.0f
            if (forward > 0.0f) {
                forward = 1.0f
            } else if (forward < 0.0f) {
                forward = -1.0f
            }
        }
        val sin = sin(Math.toRadians((yaw + 90.0f).toDouble()))
        val cos = cos(Math.toRadians((yaw + 90.0f).toDouble()))
        val posX = forward * speed * cos + side * speed * sin
        val posZ = forward * speed * sin - side * speed * cos
        return doubleArrayOf(posX, posZ)
    }

    fun getLegitRotations(vec: Vec3d): FloatArray {
        val eyesPos = getEyesPos()
        val diffX = vec.x - eyesPos.x
        val diffY = vec.y - eyesPos.y
        val diffZ = vec.z - eyesPos.z
        val diffXZ = hypot(diffX, diffZ)
        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f
        var pitch = -Math.toDegrees(atan2(diffY, diffXZ)).toFloat()
        if (pitch > 90.0f) {
            pitch = 90.0f
        } else if (pitch < -90.0f) {
            pitch = -90.0f
        }
        val player = Wrapper.player ?: return floatArrayOf(0f, 0f)
        return floatArrayOf(
            player.yaw + MathHelper.wrapDegrees(yaw - player.yaw),
            player.pitch + MathHelper.wrapDegrees(pitch - player.pitch)
        )
    }

    fun getEyesPos(): Vec3d {
        return Wrapper.player?.let {
            Vec3d(it.pos.x, it.pos.y + it.getEyeHeight(it.pose), it.pos.z)
        } ?: Vec3d.ZERO
    }

    fun normalizeAngle(angleIn: Double): Double {
        var angle = angleIn
        angle %= 360.0
        if (angle >= 180.0) {
            angle -= 360.0
        }
        if (angle < -180.0) {
            angle += 360.0
        }
        return angle
    }

    fun normalizeAngle(angleIn: Float): Float {
        var angle = angleIn
        angle %= 360.0f

        if (angle >= 180.0f) {
            angle -= 360.0f
        } else if (angle < -180.0f) {
            angle += 360.0f
        }

        return angle
    }

    fun SafeClientEvent.fovCheck(pos: Vec3d, fov: Float): Boolean {
        return abs(player.renderYaw - getRotationTo(pos).x) <= fov
    }
}