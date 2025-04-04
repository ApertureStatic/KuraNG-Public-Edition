package dev.dyzjct.kura.manager

import base.utils.concurrent.threads.runSafe
import base.utils.math.toVec3dCenter
import base.utils.math.vector.Vec2f
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.module.modules.client.AntiCheat
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.rotation.Rotation
import dev.dyzjct.kura.utils.rotation.RotationUtils.getRotationToVec2f
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d


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
                    event.setRotation(yaw, pitch)
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
    fun rotationTo(yaw: Float, pitch: Float) {
        resetTimer.reset()
        rotateYaw = yaw
        rotatePitch = pitch
    }

    @JvmStatic
    fun rotationTo(vec2f: Vec2f) {
        val rotation = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) vec2f.getRotation() else Rotation(
            vec2f.x,
            vec2f.y
        ).fixedSensitivity()
        resetTimer.reset()
        rotateYaw = rotation.yaw
        rotatePitch = rotation.pitch
    }

    @JvmStatic
    fun rotationTo(rotation: Rotation) {
        resetTimer.reset()
        rotateYaw = rotation.yaw
        rotatePitch = rotation.pitch
    }

    @JvmStatic
    fun rotationTo(blockPos: BlockPos, side: Boolean = false) {
        runSafe {
            resetTimer.reset()
            rotateYaw = getRotationToVec2f(blockPos.toVec3dCenter(), side).x
            rotatePitch = getRotationToVec2f(blockPos.toVec3dCenter(), side).y
        }
    }

    @JvmStatic
    fun rotationTo(vec3d: Vec3d, side: Boolean = false) {
        runSafe {
            val rotation = getRotationToVec2f(vec3d, side)
            resetTimer.reset()
            rotateYaw = rotation.x
            rotatePitch = rotation.y
        }
    }
}