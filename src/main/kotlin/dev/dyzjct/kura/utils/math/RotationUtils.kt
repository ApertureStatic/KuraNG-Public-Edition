package dev.dyzjct.kura.utils.math

import dev.dyzjct.kura.utils.animations.toDegree
import base.system.event.SafeClientEvent
import base.utils.entity.EntityUtils.eyePosition
import base.utils.world.getMiningSide
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import team.exception.melon.util.math.toBlockPos
import team.exception.melon.util.math.vector.Vec2f
import kotlin.math.*

object RotationUtils {
    fun calcAbsAngleDiff(a: Float, b: Float): Float {
        return abs(a - b) % 180.0f
    }

    fun calcAngleDiff(a: Float, b: Float): Float {
        val diff = a - b
        return normalizeAngle(diff)
    }

    fun SafeClientEvent.faceEntityClosest(entity: Entity) {
        val rotation = getRotationToEntityClosest(entity)
        player.setRotation(rotation)
    }

    fun SafeClientEvent.getRelativeRotation(entity: Entity): Float {
        return getRelativeRotation(entity.eyePosition)
    }

    fun SafeClientEvent.getRelativeRotation(posTo: Vec3d): Float {
        return getRotationDiff(getRotationTo(posTo), Vec2f.ofEntityRotation(player))
    }

    fun getRotationDiff(r1: Vec2f, r2: Vec2f): Float {
        val r1Radians = r1.toRadians()
        val r2Radians = r2.toRadians()
        return acos(
            cos(r1Radians.y) * cos(r2Radians.y) * cos(r1Radians.x - r2Radians.x) + sin(r1Radians.y) * sin(
                r2Radians.y
            )
        ).toDegree()
    }

    fun SafeClientEvent.getRotationToEntityClosest(entity: Entity): Vec2f {
        val box = entity.boundingBox

        val eyePos = player.eyePosition

        if (player.boundingBox.intersects(box)) {
            return getRotationTo(eyePos, box.center)
        }

        val x = eyePos.x.coerceIn(box.minX, box.maxX)
        val y = eyePos.y.coerceIn(box.minY, box.maxY)
        val z = eyePos.z.coerceIn(box.minZ, box.maxZ)

        val hitVec = Vec3d(x, y, z)
        return getRotationTo(eyePos, hitVec)
    }

    fun SafeClientEvent.getRotationToEntity(entity: Entity): Vec2f {
        return getRotationTo(entity.pos)
    }

    /**
     * Get rotation from a player position to another position vector
     *
     * @param posTo Calculate rotation to this position vector
     */
    fun SafeClientEvent.getRotationTo(posTo: Vec3d, side: Boolean = false): Vec2f {
        var posToBetter: Vec3d? = null
        if (side) getMiningSide(posTo.toBlockPos())?.let { side ->
            posToBetter = posTo.toBlockPos().offset(side).toCenterPos()
                .add(Vec3d(side.opposite.vector.x * 0.5, side.opposite.vector.y * 0.5, side.opposite.vector.z * 0.5))
        }
        return getRotationTo(
            player.pos.add(0.0, player.getEyeHeight(player.pose).toDouble(), 0.0),
            posToBetter ?: posTo
        )
    }

    fun SafeClientEvent.getYawTo(posTo: Vec3d): Float {
        val vec = posTo.subtract(player.eyePosition)
        return normalizeAngle((atan2(vec.z, vec.x).toDegree() - 90.0).toFloat())
    }

    /**
     * Get rotation from a position vector to another position vector
     *
     * @param posFrom Calculate rotation from this position vector
     * @param posTo Calculate rotation to this position vector
     */
    fun getRotationTo(posFrom: Vec3d, posTo: Vec3d): Vec2f {
        return getRotationFromVec(posTo.subtract(posFrom))
    }

    fun getRotationFromVec(vec: Vec3d): Vec2f {
        val xz = hypot(vec.x, vec.z)
        val yaw = normalizeAngle(atan2(vec.z, vec.x).toDegree() - 90.0)
        val pitch = normalizeAngle(-atan2(vec.y, xz).toDegree())
        return Vec2f(yaw, pitch)
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

    fun normalizeAnglePitch(angleIn: Float): Float {
        var angle = angleIn
        angle %= 180.0f

        if (angle >= 90.0f) {
            angle -= 180.0f
        } else if (angle < -90.0f) {
            angle += 180.0f
        }

        return angle
    }

    fun ClientPlayerEntity.setRotation(rotation: Vec2f) {
        this.setYaw(rotation.x)
        this.setPitch(rotation.y)
    }

    fun ClientPlayerEntity.setYaw(yaw: Float) {
        this.yaw += normalizeAngle(yaw - this.yaw)
    }

    fun ClientPlayerEntity.setPitch(pitch: Float) {
        this.pitch = (this.pitch + normalizeAngle(pitch - this.pitch)).coerceIn(-90.0f, 90.0f)
    }

    fun ClientPlayerEntity.legitRotation(rotation: Vec2f): Vec2f {
        return Vec2f(legitYaw(rotation.x), legitPitch(rotation.y))
    }

    fun ClientPlayerEntity.legitYaw(yaw: Float): Float {
        return this.yaw + normalizeAngle(yaw - this.yaw)
    }

    fun ClientPlayerEntity.legitPitch(pitch: Float): Float {
        return (this.pitch + normalizeAngle(pitch - this.pitch)).coerceIn(-90.0f, 90.0f)
    }

    val Direction.yaw: Float
        get() = when (this) {
            Direction.NORTH -> -180.0f
            Direction.SOUTH -> 0.0f
            Direction.EAST -> -90.0f
            Direction.WEST -> 90.0f
            else -> 0.0f
        }
}