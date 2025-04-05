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
import dev.dyzjct.kura.utils.rotation.RotationUtils.getRotationToVec2f
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.*


object RotationManager : AlwaysListening {
    private val resetTimer = TimerUtils()
    var rotateYaw: Float? = null
    var rotatePitch: Float? = null
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
                        event.setRenderRotation(yaw, pitch)
                    } else event.setRotation(yaw, pitch)
                }
            }
        }


        safeEventListener<PacketEvents.Send>(Int.MAX_VALUE) { event ->
            if (event.packet is PlayerMoveC2SPacket) {
                rotateYaw?.let { yaw ->
                    rotatePitch?.let { pitch ->
                        event.packet.yaw = yaw
                        event.packet.pitch = pitch
                    }
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
    fun rotationTo(yaw: Float, pitch: Float, steps: Float = 1f) {
        resetTimer.reset()
        injectStep(Vec2f(yaw, pitch), steps)
        rotateYaw = yaw
        rotatePitch = pitch
    }

    @JvmStatic
    fun rotationTo(vec2f: Vec2f, steps: Float = 1f) {
        injectStep(vec2f, steps)
        resetTimer.reset()
        rotateYaw = injectStep(vec2f, steps).x
        rotatePitch = injectStep(vec2f, steps).y
    }

    @JvmStatic
    fun rotationTo(rotation: Rotation, steps: Float = 1f) {
        resetTimer.reset()
        rotateYaw = injectStep(Vec2f(rotation.yaw, rotation.pitch), steps).x
        rotatePitch = injectStep(Vec2f(rotation.yaw, rotation.pitch), steps).y
    }

    @JvmStatic
    fun rotationTo(blockPos: BlockPos, side: Boolean = false, steps: Float = 1f) {
        runSafe {
            resetTimer.reset()
            var vec = getRotationToVec2f(blockPos.toVec3dCenter(), side)
            vec = injectStep(vec, steps)
            rotateYaw = vec.x
            rotatePitch = vec.y
        }
    }

    @JvmStatic
    fun rotationTo(vec3d: Vec3d, side: Boolean = false, steps: Float = 1f) {
        runSafe {
            var vec = getRotationToVec2f(vec3d, side)
            vec = injectStep(vec, steps)
            resetTimer.reset()
            rotateYaw = vec.x
            rotatePitch = vec.y
        }
    }

    fun SafeClientEvent.inFov(blockPos: BlockPos, fov: Float, side: Boolean = false): Boolean {
        return inFov(
            getRotationToVec2f(blockPos.toVec3dCenter(), side).x,
            getRotationToVec2f(blockPos.toVec3dCenter(), side).y,
            fov
        )
    }

    fun SafeClientEvent.inFov(vec3d: Vec3d, fov: Float, side: Boolean = false): Boolean {
        return inFov(getRotationToVec2f(vec3d, side).x, getRotationToVec2f(vec3d, side).y, fov)
    }

    fun SafeClientEvent.inFov(yaw: Float, pitch: Float, fov: Float): Boolean {
        if (rotateYaw == null) return false
        return MathHelper.angleBetween(yaw, rotateYaw!!) + abs((pitch - rotatePitch!!).toDouble()) <= fov
    }

    fun SafeClientEvent.injectStep(vec: Vec3d, steps: Float): Vec2f {
        if (rotateYaw == null || rotatePitch == null) return Vec2f(vec.x, vec.y)
        var yawDelta = MathHelper.wrapDegrees(
            MathHelper.wrapDegrees(
                Math.toDegrees(
                    atan2(
                        vec.z - player.z,
                        (vec.x - player.x)
                    )
                ) - 90
            ).toFloat() - rotateYaw!!
        )
        var pitchDelta = ((-Math.toDegrees(
            atan2(
                vec.y - (player.getPos().y + player.getEyeHeight(player.pose)),
                sqrt((vec.x - player.x).pow(2.0) + (vec.z - player.z).pow(2.0))
            )
        )).toFloat() - rotatePitch!!)

        val angleToRad = Math.toRadians((27 * (player.age % 30)).toDouble()).toFloat()
        yawDelta = (yawDelta + sin(angleToRad.toDouble()) * 3).toFloat() + MathUtil.random(-1f, 1f)
        pitchDelta += MathUtil.random(-0.6f, 0.6f)

        if (yawDelta > 180) yawDelta -= 180

        val yawStepVal = 180 * steps

        val clampedYawDelta = MathHelper.clamp(MathHelper.abs(yawDelta), -yawStepVal, yawStepVal)
        val clampedPitchDelta = MathHelper.clamp(pitchDelta, -45f, 45f)

        val newYaw = rotateYaw!! + (if (yawDelta > 0) clampedYawDelta else -clampedYawDelta)
        val newPitch = MathHelper.clamp(rotatePitch!! + clampedPitchDelta, -90.0f, 90.0f)

        val gcdFix: Double = ((mc.options.mouseSensitivity.value * 0.6 + 0.2).pow(3.0)) * 1.2

        return Vec2f(
            (newYaw - (newYaw - rotateYaw!!) % gcdFix).toFloat(),
            (newPitch - (newPitch - rotatePitch!!) % gcdFix).toFloat()
        )
    }

    fun injectStep(vec2f: Vec2f, step: Float): Vec2f {
        if (rotateYaw == null || rotatePitch == null) return vec2f
        var steps = step
        var newVec = Vec2f(vec2f.x, vec2f.y)
        if (steps < 0.01f) steps = 0.01f
        if (steps > 1) steps = 1f
        if (steps < 1) {
            var diff = MathHelper.angleBetween(newVec.x, rotateYaw!!)
            if (abs(diff.toDouble()) > 180 * steps) {
                newVec = Vec2f(((rotateYaw!! + (diff * ((180 * steps) / abs(diff.toDouble())))).toFloat()), newVec.y)
            }
            diff = newVec.y - rotatePitch!!
            if (abs(diff.toDouble()) > 90 * steps) {
                newVec = Vec2f(newVec.x, ((rotatePitch!! + (diff * ((90 * steps) / abs(diff.toDouble())))).toFloat()))
            }
        }
        return newVec
    }
}