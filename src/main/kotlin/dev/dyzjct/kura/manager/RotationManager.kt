package dev.dyzjct.kura.manager

import base.utils.concurrent.threads.runSafe
import base.utils.math.toVec3dCenter
import base.utils.math.vector.Vec2f
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.module.modules.client.AntiCheat
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.math.MathUtil
import dev.dyzjct.kura.utils.rotation.Rotation
import dev.dyzjct.kura.utils.rotation.RotationUtils.getFixedRotationTo
import dev.dyzjct.kura.utils.rotation.RotationUtils.getRotationToRotation
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.network.packet.s2c.play.PositionFlag
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


object RotationManager : AlwaysListening {
    private val resetTimer = TimerUtils()
    var nextYaw: Float = 0f
    var nextPitch: Float = 0f
    var rotateYaw: Float? = null
    var rotatePitch: Float? = null
    var lastYaw: Float = 0f
    var lastPitch: Float = 0f
    var lastGround = true
    private var stop = false

    fun onInit() {
        safeEventListener<PlayerMotionEvent>(Int.MAX_VALUE) { event ->
            if (stop) {
                rotateYaw = null
                rotatePitch = null
                return@safeEventListener
            }
            if (resetTimer.passed(500)) {
                rotateYaw = null
                rotatePitch = null
                return@safeEventListener
            }
            rotateYaw?.let { yaw ->
                rotatePitch?.let { pitch ->
                    event.setRotation(yaw, pitch)
                }
            }
        }


        safeEventListener<PacketEvents.Send>(Int.MAX_VALUE) { event ->
            if (event.packet is PlayerMoveC2SPacket) {
                if (event.packet.changesLook()) {
                    lastYaw = event.packet.getYaw(lastYaw)
                    lastPitch = event.packet.getPitch(lastPitch)
                }
                lastGround = event.packet.isOnGround
            }
        }
        safeEventListener<PacketEvents.Receive>(Int.MAX_VALUE) { event ->
            if (event.packet is PlayerPositionLookS2CPacket) {
                if (event.packet.flags.contains(PositionFlag.X_ROT)) {
                    lastYaw += event.packet.getYaw()
                } else {
                    lastYaw = event.packet.getYaw()
                }
                if (event.packet.flags.contains(PositionFlag.Y_ROT)) {
                    lastPitch += event.packet.getPitch()
                } else {
                    lastPitch = event.packet.getPitch()
                }
            }
        }
    }

    fun stopRotation() {
        stop = true
    }

    fun startRotation() {
        stop = false
    }

    @JvmStatic
    fun rotationTo(yaw: Float, pitch: Float) {
        val rotation = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) Rotation(yaw, pitch) else Rotation(
            yaw,
            pitch
        ).fixedSensitivity()
        resetTimer.reset()
        runSafe {
            snapAt(rotation.yaw, rotation.pitch)
        }
        rotateYaw = rotation.yaw
        rotatePitch = rotation.pitch
    }

    @JvmStatic
    fun rotationTo(vec2f: Vec2f) {
        val rotation = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) vec2f.getRotation() else Rotation(
            vec2f.x,
            vec2f.y
        ).fixedSensitivity()
        resetTimer.reset()
        runSafe {
            snapAt(rotation.yaw, rotation.pitch)
        }
        rotateYaw = rotation.yaw
        rotatePitch = rotation.pitch
    }

    @JvmStatic
    fun rotationTo(rotation: Rotation) {
        resetTimer.reset()
        runSafe {
            snapAt(rotation.yaw, rotation.pitch)
        }
        rotateYaw = rotation.yaw
        rotatePitch = rotation.pitch
    }

    @JvmStatic
    fun rotationTo(blockPos: BlockPos, side: Boolean = false) {
        runSafe {
            val fixedRotation = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) getRotationToRotation(
                blockPos.toVec3dCenter(),
                side
            ) else getFixedRotationTo(blockPos.toVec3dCenter(), side)
            resetTimer.reset()
            snapAt(fixedRotation.yaw, fixedRotation.pitch)
            rotateYaw = fixedRotation.yaw
            rotatePitch = fixedRotation.pitch
        }
    }

    @JvmStatic
    fun rotationTo(vec3d: Vec3d, side: Boolean = false) {
        runSafe {
            val fixedRotation = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) getRotationToRotation(
                vec3d,
                side
            ) else getFixedRotationTo(vec3d, side)
            resetTimer.reset()
            snapAt(fixedRotation.yaw, fixedRotation.pitch)
            rotateYaw = fixedRotation.yaw
            rotatePitch = fixedRotation.pitch
        }
    }

    fun SafeClientEvent.snapAt(yaw: Float, pitch: Float) {
        rotateYaw = yaw
        rotatePitch = pitch
        if (AntiCheat.ac == AntiCheat.AntiCheats.GrimAC) {
            connection.sendPacket(
                PlayerMoveC2SPacket.Full(
                    player.x,
                    player.y,
                    player.z,
                    yaw,
                    pitch,
                    player.isOnGround
                )
            )
        } else {
            connection.sendPacket(LookAndOnGround(yaw, pitch, player.isOnGround))
        }
    }

    fun SafeClientEvent.snapAt(directionVec: Vec3d) {
        val angle: FloatArray = getRotation(directionVec)
        if (AntiCheat.noSpamRotation) {
            if (MathHelper.angleBetween(
                    angle[0],
                    lastYaw
                ) < AntiCheat.fov && Math.abs(
                    angle[1] - lastPitch
                ) < AntiCheat.fov
            ) {
                return
            }
        }
        snapAt(angle[0], angle[1])
    }

    fun getRotation(eyesPos: Vec3d, vec: Vec3d): FloatArray {
        val diffX = vec.x - eyesPos.x
        val diffY = vec.y - eyesPos.y
        val diffZ = vec.z - eyesPos.z
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
        val yaw = Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90.0f
        val pitch = (-Math.toDegrees(atan2(diffY, diffXZ))).toFloat()
        return floatArrayOf(MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch))
    }

    fun SafeClientEvent.getRotation(vec: Vec3d): FloatArray {
        val eyesPos: Vec3d = player.eyePos
        return getRotation(eyesPos, vec)
    }

    fun SafeClientEvent.injectStep(vec: Vec3d, steps: Float): FloatArray {
        val currentYaw = if (AntiCheat.forceSync) lastYaw else rotateYaw!!
        val currentPitch = if (AntiCheat.forceSync) lastPitch else rotatePitch!!

        var yawDelta = MathHelper.wrapDegrees(
            MathHelper.wrapDegrees(
                Math.toDegrees(
                    atan2(
                        vec.z - player.z,
                        (vec.x - player.x)
                    )
                ) - 90
            ).toFloat() - currentYaw
        )
        var pitchDelta = ((-Math.toDegrees(
            atan2(
                vec.y - (player.getPos().y + player.getEyeHeight(player.pose)), sqrt(
                    (vec.x - player.x).pow(2.0) + (vec.z - player.z).pow(2.0)
                )
            )
        )).toFloat() - currentPitch)

        val angleToRad = Math.toRadians(27.0 * (player.age % 30.0)).toFloat()
        yawDelta = (yawDelta + sin(angleToRad.toDouble()) * 3).toFloat() + MathUtil.random(-1f, 1f)
        pitchDelta += MathUtil.random(-0.6f, 0.6f)

        if (yawDelta > 180) yawDelta -= 180

        val yawStepVal = 180 * steps

        val clampedYawDelta = MathHelper.clamp(MathHelper.abs(yawDelta), -yawStepVal, yawStepVal)
        val clampedPitchDelta = MathHelper.clamp(pitchDelta, -45f, 45f)

        val newYaw = currentYaw + (if (yawDelta > 0) clampedYawDelta else -clampedYawDelta)
        val newPitch = MathHelper.clamp(currentPitch + clampedPitchDelta, -90.0f, 90.0f)

        val gcdFix: Double = ((mc.options.mouseSensitivity.value * 0.6 + 0.2).pow(3.0)) * 1.2

        return floatArrayOf(
            (newYaw - (newYaw - currentYaw) % gcdFix).toFloat(),
            (newPitch - (newPitch - currentPitch) % gcdFix).toFloat()
        )
    }
}