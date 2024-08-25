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
import dev.dyzjct.kura.module.modules.client.CombatSystem.rotationDelay
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.rotation.Rotation
import dev.dyzjct.kura.utils.rotation.RotationUtils.getFixedRotationTo
import dev.dyzjct.kura.utils.rotation.RotationUtils.normalizeAngle
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


object RotationManager : AlwaysListening {
    private val resetTimer = TimerUtils()
    var rotateYaw: Float? = null
    private var rotationYaw: Float? = null
    private var rotatePitch: Float? = null
    private var playerLastRotation: Rotation? = null
    private var motionTimer = TimerUtils()
    private var packetTimer = TimerUtils()
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
                    if (motionTimer.tickAndReset(rotationDelay())) {
                        val smoothedRotation = getSmoothRotation(
                            playerLastRotation ?: Rotation(player.yaw, player.pitch),
                            Rotation(yaw, pitch),
                            CombatSystem.rotationSpeed
                        )
                        event.setRotation(smoothedRotation.yaw, pitch)
                    }
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
                if (packetTimer.tickAndReset(rotationDelay())) {
                    val smoothedRotation = getSmoothRotation(
                        playerLastRotation ?: Rotation(player.yaw, player.pitch),
                        Rotation(yaw, pitch),
                        CombatSystem.rotationSpeed
                    )
                    when (event.packet) {
                        is PlayerMoveC2SPacket -> {
                            event.packet.yaw = smoothedRotation.yaw
                            event.packet.pitch = pitch
                        }
                    }
                }
            }
        }
    }

    private fun getSmoothRotation(lastRotation: Rotation, targetRotation: Rotation, speed: Double): Rotation {
        var yaw = targetRotation.yaw
        var pitch = targetRotation.pitch
        val lastYaw = lastRotation.yaw
        val lastPitch = lastRotation.pitch

        if (speed != 0.0) {
            val rotationSpeed = speed.toFloat()

            val deltaYaw = normalizeAngle(targetRotation.yaw - lastRotation.yaw)
            val deltaPitch = (pitch - lastPitch).toDouble()

            val distance = sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch)
            val distributionYaw = abs(deltaYaw / distance)
            val distributionPitch = abs(deltaPitch / distance)

            val maxYaw = rotationSpeed * distributionYaw
            val maxPitch = rotationSpeed * distributionPitch

            val moveYaw = max(min(deltaYaw.toDouble(), maxYaw), -maxYaw).toFloat()
            val movePitch = max(min(deltaPitch, maxPitch), -maxPitch).toFloat()

            yaw = lastYaw + moveYaw
            pitch = lastPitch + movePitch
        }

        val randomise = Math.random() > 0.8

        for (i in 1..(2 + Math.random() * 2).toInt()) {
            if (randomise) {
                yaw += ((Math.random() - 0.5) / 100000000).toFloat()
                pitch -= (Math.random() / 200000000).toFloat()
            }

            /*
             * Fixing GCD
             */
            val rotations = Rotation(yaw, pitch).fixedSensitivity()

            /*
             * Setting rotations
             */
            yaw = rotations.yaw
            pitch = (-90).coerceAtLeast(90.coerceAtMost(rotations.yaw.toInt())).toFloat()
        }
        playerLastRotation = Rotation(yaw, pitch)
        return Rotation(yaw, pitch)
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