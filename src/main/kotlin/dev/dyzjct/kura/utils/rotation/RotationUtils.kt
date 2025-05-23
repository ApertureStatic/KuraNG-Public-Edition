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
import net.minecraft.util.math.random.Random
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
        return getRotationDiff(getRotationToVec2f(posTo), Vec2f.ofEntityRotation(player))
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
            return getRotationToVec2f(eyePos, box.center)
        }

        val x = eyePos.x.coerceIn(box.minX, box.maxX)
        val y = eyePos.y.coerceIn(box.minY, box.maxY)
        val z = eyePos.z.coerceIn(box.minZ, box.maxZ)

        val hitVec = Vec3d(x, y, z)
        return getRotationToVec2f(eyePos, hitVec)
    }

    fun SafeClientEvent.getRotationToEntity(entity: Entity): Vec2f {
        return getRotationToVec2f(entity.pos)
    }

    fun SafeClientEvent.fovCheck(pos: Vec3d, fov: Float): Boolean {
        return abs(normalizeAngle(player.yaw) - getRotationToVec2f(pos).x) <= fov
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

    fun SafeClientEvent.getRotationToVec2f(posTo: Vec3d, side: Boolean = false): Vec2f {
        var posToBetter: Vec3d? = null
        if (side) getMiningSide(posTo.toBlockPos())?.let { side ->
            posToBetter = posTo.toBlockPos().offset(side).toCenterPos()
                .add(Vec3d(side.opposite.vector.x * 0.5, side.opposite.vector.y * 0.5, side.opposite.vector.z * 0.5))
        }
        return getRotationToVec2f(
            player.pos.add(0.0, player.getEyeHeight(player.pose).toDouble(), 0.0),
            posToBetter ?: posTo
        )
    }

    fun getRotationToVec2f(posFrom: Vec3d, posTo: Vec3d): Vec2f {
        return getRotationFromVec(posTo.subtract(posFrom))
    }

    fun getRotationFromVec(vec: Vec3d): Vec2f {
        val xz = hypot(vec.x, vec.z)
        val yaw = normalizeAngle(atan2(vec.z, vec.x).toDegree() - 90.0)
        val pitch = normalizeAngle(-atan2(vec.y, xz).toDegree())
        return Vec2f(yaw, pitch)
    }

    fun getRotations(entity: Entity?, x: Double, y: Double, z: Double, maxJitter: Float): Vec2f {
        // 防御性编程：检查所有输入有效性
        if (entity == null ||
            java.lang.Double.isNaN(x) || java.lang.Double.isNaN(y) || java.lang.Double.isNaN(z) || !java.lang.Double.isFinite(
                x
            ) || !java.lang.Double.isFinite(y) || !java.lang.Double.isFinite(z)
        ) {
            return Vec2f(0f, 0f)
        }

        // 获取精确眼部坐标（考虑潜行、游泳等姿态）
        val eyePos = entity.eyePos

        // 计算坐标差（使用final防止意外修改）
        val deltaX = x - eyePos.x
        val deltaY = (y - eyePos.y) * -1.0 // 正确转换为屏幕空间Y轴方向
        val deltaZ = z - eyePos.z

        // 计算水平距离并防止极小值
        val horizontalSq = deltaX * deltaX + deltaZ * deltaZ
        val horizontalDistance = if (horizontalSq < 1e-14) 1e-7 else sqrt(horizontalSq)

        // 使用atan2计算角度（避免手动计算导致的象限错误）
        var yaw = Math.toDegrees(atan2(deltaZ, deltaX)).toFloat() - 90.0f
        var pitch = Math.toDegrees(atan2(deltaY, horizontalDistance)).toFloat()

        // 角度规范化（使用Minecraft原生方法）
        yaw = MathHelper.wrapDegrees(yaw)
        pitch = MathHelper.wrapDegrees(pitch)

        // 安全钳制pitch角度（保留0.1度缓冲防止万向节锁）
        pitch = MathHelper.clamp(pitch, -89.9f, 89.9f)

        // 受控抖动系统（使用实体随机数生成器保证同步）
        if (maxJitter > 0) {
            val rand: Random = entity.random
            val effectiveJitter = min(maxJitter.toDouble(), 5.0).toFloat() // 安全限制最大抖动幅度

            // 生成正态分布的抖动值（更自然的随机效果）
            val yawJitter = (rand.nextFloat() - 0.5f) * 2 * effectiveJitter
            val pitchJitter = (rand.nextFloat() - 0.5f) * 2 * effectiveJitter

            yaw += yawJitter
            pitch += pitchJitter

            // 最终角度验证
            yaw = MathHelper.wrapDegrees(yaw)
            pitch = MathHelper.clamp(MathHelper.wrapDegrees(pitch), -89.9f, 89.9f)
        }

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