package dev.dyzjct.kura.utils.rotation

import base.utils.Wrapper
import base.utils.entity.EntityUtils.eyePosition
import base.utils.math.toBlockPos
import base.utils.math.vector.Vec2f
import base.utils.world.getMiningSide
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.module.modules.movement.Blink
import dev.dyzjct.kura.utils.animations.toDegree
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.*

object RotationUtils : AlwaysListening {
    private var actualServerRotation = Rotation.ZERO
    private var theoreticalServerRotation = Rotation.ZERO
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

    fun SafeClientEvent.fovCheck(pos: Vec3d, fov: Float): Boolean {
        return abs(normalizeAngle(player.yaw) - getRotationTo(pos).x) <= fov
    }

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

    fun SafeClientEvent.getFixedRotationTo(posTo: Vec3d, side: Boolean = false): Rotation {
        var posToBetter: Vec3d? = null
        if (side) getMiningSide(posTo.toBlockPos())?.let { side ->
            posToBetter = posTo.toBlockPos().offset(side).toCenterPos()
                .add(Vec3d(side.opposite.vector.x * 0.5, side.opposite.vector.y * 0.5, side.opposite.vector.z * 0.5))
        }
        val rotation = getRotationTo(
            player.pos.add(0.0, player.getEyeHeight(player.pose).toDouble(), 0.0),
            posToBetter ?: posTo
        )
        return Rotation(rotation.x, rotation.y).fixedSensitivity()
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

    val gcd: Double
        get() {
            val f = Wrapper.minecraft.options.mouseSensitivity.value * 0.6F.toDouble() + 0.2F.toDouble()
            return f * f * f * 8.0 * 0.15F
        }


    val serverRotation: Rotation
        get() = if (Blink.packets.isNotEmpty()) theoreticalServerRotation else actualServerRotation

    init {
        safeEventListener<PacketEvents.Send> {
            val rotation = when (val packet = it.packet) {
                is PlayerMoveC2SPacket -> {
                    // If we are not changing the look, we don't need to update the rotation
                    // but, we want to handle slow start triggers
                    if (!packet.changeLook) {
                        return@safeEventListener
                    }

                    Rotation(packet.yaw, packet.pitch)
                }

                is PlayerPositionLookS2CPacket -> Rotation(packet.yaw, packet.pitch)
                else -> return@safeEventListener
            }

            // This normally applies to Modules like Blink, BadWifi, etc.
            if (!it.cancelled) {
                actualServerRotation = rotation
            }

            theoreticalServerRotation = rotation
        }
    }

    fun rotMove(target: Float, current: Float, diff: Float): Float {
        return rotMoveNoRandom(target, current, diff)
    }

    fun rotMoveNoRandom(target: Float, current: Float, diff: Float): Float {
        var delta: Float
        if (target > current) {
            val dist1 = target - current
            val dist2 = current + 360 - target
            delta = if (dist1 > dist2) {  // 另一边移动更近
                -current - 360 + target
            } else {
                dist1
            }
        } else if (target < current) {
            val dist1 = current - target
            val dist2 = target + 360 - current
            delta = if (dist1 > dist2) {  // 另一边移动更近
                current + 360 + target
            } else {
                -dist1
            }
        } else {
            return current
        }

        delta = normalizeAngle(delta)

        return if (abs(delta.toDouble()) < 0.1 * Math.random() + 0.1) {
            current
        } else if (abs(delta.toDouble()) <= diff) {
            current + delta
        } else {
            if (delta < 0) {
                current - diff
            } else if (delta > 0) {
                current + diff
            } else {
                current
            }
        }
    }
}