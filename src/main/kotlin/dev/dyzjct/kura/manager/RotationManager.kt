package dev.dyzjct.kura.manager

import base.events.PacketEvents
import base.events.player.PlayerMotionEvent
import base.system.event.AlwaysListening
import base.system.event.safeEventListener
import base.utils.concurrent.threads.runSafe
import base.utils.math.toVec3dCenter
import base.utils.math.vector.Vec2f
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.math.RotationUtils.getRotationTo
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object RotationManager : AlwaysListening {
    private val rotationMap = ConcurrentHashMap<Vec2f, Boolean>()
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
//            rotationMap.toSortedMap(Comparator.comparing { (_, prio) -> prio }).forEach { (vec, _) ->
//                val packet = Vec2f(vec.x, vec.y)
//                event.setRotation(packet.x, packet.y)
//                rotationMap.remove(packet)
//            }
        }

        safeEventListener<PacketEvents.Send>(Int.MAX_VALUE) { event ->
            if (stop) return@safeEventListener
            spoofMap.forEach {
                when (event.packet) {
                    is PlayerMoveC2SPacket -> {
                        val packet = it
                        event.packet.yaw = rotateYaw
                        event.packet.pitch = rotatePitch
                    }
                }
            }
//            if (rotationMap.isNotEmpty()) {
//                when (event.packet) {
//                    is PlayerMoveC2SPacket -> {
//                        if (!rotationMap.contains(Vec2f(event.packet.yaw, event.packet.pitch))) {
//                        }
//                    }
//                }
//            }
        }
    }

    fun stopRotation() {
        stop = true
    }

    fun startRotation() {
        stop = false
    }

    @JvmStatic
    fun addRotations(yaw: Float, pitch: Float, prio: Boolean = false) {
        rotationMap[Vec2f(yaw, pitch)] = prio
        rotateYaw = yaw
        rotatePitch = pitch
        resetTimer.reset()
    }

    @JvmStatic
    fun addRotations(rotation: Vec2f, prio: Boolean = false) {
        rotationMap[rotation] = prio
        rotateYaw = rotation.x
        rotatePitch = rotation.y
        resetTimer.reset()
    }

    @JvmStatic
    fun addRotations(blockPos: BlockPos, prio: Boolean = false, side: Boolean = false) {
        runSafe {
            rotationMap[getRotationTo(blockPos.toVec3dCenter(), side)] = prio
            rotateYaw = getRotationTo(blockPos.toVec3dCenter(), side).x
            rotatePitch = getRotationTo(blockPos.toVec3dCenter(), side).y
            resetTimer.reset()
        }
    }

    @JvmStatic
    fun addRotations(vec3d: Vec3d, prio: Boolean = false, side: Boolean = false) {
        runSafe {
            rotationMap[getRotationTo(vec3d, side)] = prio
            rotateYaw = getRotationTo(vec3d, side).x
            rotatePitch = getRotationTo(vec3d, side).y
            resetTimer.reset()
        }
    }
}