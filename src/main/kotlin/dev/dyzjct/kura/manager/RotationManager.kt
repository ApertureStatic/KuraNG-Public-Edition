package dev.dyzjct.kura.manager

import base.utils.concurrent.threads.runSafe
import base.utils.math.toVec3dCenter
import base.utils.math.vector.Vec2f
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.rotation.Rotation
import dev.dyzjct.kura.utils.rotation.RotationUtils.getFixedRotationTo
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object RotationManager : AlwaysListening {
    private val resetTimer = TimerUtils()
    var rotateYaw: Float? = null
    private var rotatePitch: Float? = null
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
                    event.setRenderRotation(yaw, pitch)
                    connection.sendPacket(
                        PlayerMoveC2SPacket.Full(
                            player.x,
                            player.y,
                            player.z,
                            yaw,
                            pitch,
                            player.onGround
                        )
                    )
                }
            }
        }

        safeEventListener<PacketEvents.Send>(Int.MAX_VALUE) { event ->
            setRotation(event)
        }
    }

    private fun setRotation(event: PacketEvents) {
        if (stop) {
            rotateYaw = null
            rotatePitch = null
            return
        }
        if (resetTimer.passed(500)) {
            rotateYaw = null
            rotatePitch = null
            return
        }
        rotateYaw?.let { yaw ->
            rotatePitch?.let { pitch ->
                when (event.packet) {
                    is PlayerMoveC2SPacket -> {
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
        rotateYaw = yaw
        rotatePitch = pitch
        resetTimer.reset()
    }

    @JvmStatic
    fun rotationTo(rotation: Vec2f) {
        rotateYaw = rotation.x
        rotatePitch = rotation.y
        resetTimer.reset()
    }

    @JvmStatic
    fun rotationTo(rotation: Rotation) {
        rotateYaw = rotation.yaw
        rotatePitch = rotation.pitch
        resetTimer.reset()
    }

    @JvmStatic
    fun rotationTo(blockPos: BlockPos, side: Boolean = false) {
        runSafe {
            rotateYaw = getFixedRotationTo(blockPos.toVec3dCenter(), side).yaw
            rotatePitch = getFixedRotationTo(blockPos.toVec3dCenter(), side).pitch
            resetTimer.reset()
        }
    }

    @JvmStatic
    fun rotationTo(vec3d: Vec3d, side: Boolean = false) {
        runSafe {
            rotateYaw = getFixedRotationTo(vec3d, side).yaw
            rotatePitch = getFixedRotationTo(vec3d, side).pitch
            resetTimer.reset()
        }
    }
}