package dev.dyzjct.kura.manager

import base.utils.concurrent.threads.runSafe
import base.utils.math.toVec3dCenter
import base.utils.math.vector.Vec2f
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.StageType
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.rotation.Rotation
import dev.dyzjct.kura.utils.rotation.RotationUtils.getFixedRotationTo
import dev.dyzjct.kura.utils.rotation.RotationUtils.normalizeAngle
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d


object RotationManager : AlwaysListening {
    private val resetTimer = TimerUtils()
    var rotateYaw: Float? = null
    private var rotatePitch: Float? = null
    private var rotationYaw: Float? = null
    private var smoothed = false
    private var stop = false

    fun onInit() {
        safeEventListener<PlayerMotionEvent>(Int.MAX_VALUE) { event ->
            if (event.stageType != StageType.END) return@safeEventListener
            if (stop) {
                rotateYaw = null
                rotatePitch = null
                rotationYaw = null
                return@safeEventListener
            }
            if (resetTimer.passed(500)) {
                rotateYaw = null
                rotatePitch = null
                rotationYaw = null
                return@safeEventListener
            }
            rotateYaw?.let { yaw ->
                rotatePitch?.let { pitch ->
                    val clamped = 1.coerceIn(-CombatSystem.rotationSpeed, CombatSystem.rotationSpeed)
                    event.setRotation(
                        normalizeAngle(yaw + if (CombatSystem.smooth) clamped else 0), pitch
                    )
                }
            }
        }


        safeEventListener<PacketEvents.Send>(Int.MAX_VALUE) { event ->
            setRotation(event)
        }
    }

    private fun SafeClientEvent.setRotation(event: PacketEvents) {
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
                val clamped = 1.coerceIn(-CombatSystem.rotationSpeed, CombatSystem.rotationSpeed)
                when (event.packet) {
                    is PlayerMoveC2SPacket -> {
                        event.packet.yaw = normalizeAngle(yaw + if (CombatSystem.smooth) clamped else 0)
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
        val fixed = Rotation(yaw, pitch).fixedSensitivity()
        rotateYaw = fixed.yaw
        rotatePitch = fixed.pitch
        resetTimer.reset()
    }

    @JvmStatic
    fun rotationTo(rotation: Vec2f) {
        val fixed = Rotation(rotation.x, rotation.y).fixedSensitivity()
        rotateYaw = fixed.yaw
        rotatePitch = fixed.pitch
        resetTimer.reset()
    }

    @JvmStatic
    fun rotationTo(rotation: Rotation) {
        rotateYaw = rotation.fixedSensitivity().yaw
        rotatePitch = rotation.fixedSensitivity().pitch
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