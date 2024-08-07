package dev.dyzjct.kura.manager

import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.safeEventListener
import base.utils.concurrent.threads.runSafe
import base.utils.math.toVec3dCenter
import base.utils.math.vector.Vec2f
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.rotation.Rotation
import dev.dyzjct.kura.utils.rotation.RotationUtils.getFixedRotationTo
import dev.dyzjct.kura.utils.rotation.RotationUtils.getRotationTo
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.concurrent.CopyOnWriteArrayList

object RotationManager : AlwaysListening {
    private val resetTimer = TimerUtils()
    private var rotateYaw = 0f
    private var rotatePitch = 0f
    private var spoofMap = CopyOnWriteArrayList<Vec2f>()
    private var stop = false

    fun onInit() {
        safeEventListener<PlayerMotionEvent>(Int.MAX_VALUE) { event ->
            if (stop) return@safeEventListener
            if (resetTimer.passed(500)) return@safeEventListener
            event.setRotation(rotateYaw, rotatePitch)
        }

        safeEventListener<PacketEvents.Send>(Int.MAX_VALUE) { event ->
            if (stop) return@safeEventListener
            spoofMap.forEach { _ ->
                when (event.packet) {
                    is PlayerMoveC2SPacket -> {
                        event.packet.yaw = rotateYaw
                        event.packet.pitch = rotatePitch
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