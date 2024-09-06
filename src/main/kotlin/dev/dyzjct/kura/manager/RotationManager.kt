package dev.dyzjct.kura.manager

import base.utils.concurrent.threads.runSafe
import base.utils.math.toVec3dCenter
import base.utils.math.vector.Vec2f
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.Event
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.module.modules.client.AntiCheat
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.rotation.Rotation
import dev.dyzjct.kura.utils.rotation.RotationUtils.getFixedRotationTo
import dev.dyzjct.kura.utils.rotation.RotationUtils.getRotationToRotation
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
    private var rotationPitch: Float? = null
    private var playerLastRotation: Rotation? = null
    private var stop = false

    fun onInit() {
        safeEventListener<PlayerMotionEvent>(Int.MAX_VALUE) { event ->
            if (stop) {
                rotateYaw = null
                rotationPitch = null
                rotationYaw = null
                return@safeEventListener
            }
            if (resetTimer.passed(500)) {
                rotateYaw = null
                rotationPitch = null
                rotationYaw = null
                return@safeEventListener
            }
            rotateYaw?.let { yaw ->
                rotationPitch?.let { pitch ->
                    val smoothedRotation = getSmoothRotation(
                        playerLastRotation ?: Rotation(player.yaw, player.pitch),
                        Rotation(yaw, pitch),
                        CombatSystem.rotationSpeed
                    )
                    event.setRotation(smoothedRotation.yaw, pitch)
                }
            }
        }


        safeEventListener<PacketEvents.Send>(Int.MAX_VALUE) { event ->
            if (CombatSystem.setRotation) setRotation(event)
        }
    }

    private fun SafeClientEvent.setRotation(event: PacketEvents) {
        if (stop) {
            rotateYaw = null
            rotationPitch = null
            return
        }
        if (resetTimer.passed(500)) {
            rotateYaw = null
            rotationPitch = null
            return
        }
        rotateYaw?.let { yaw ->
            rotationPitch?.let { pitch ->
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
    fun rotationTo(yaw: Float, pitch: Float, event: Event? = null) {
        val fixed = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) Rotation(yaw, pitch) else Rotation(
            yaw,
            pitch
        ).fixedSensitivity()
        if (event != null && AntiCheat.ac == AntiCheat.AntiCheats.GrimAC) {
            runSafe { doRotate(event, yaw, pitch) }
        } else if (AntiCheat.ac == AntiCheat.AntiCheats.Legit) {
            runSafe {
                player.yaw = fixed.yaw
                player.pitch = fixed.pitch
            }
        } else {
            resetTimer.reset()
            rotateYaw = fixed.yaw
            rotationPitch = fixed.pitch
        }
    }

    @JvmStatic
    fun rotationTo(rotation: Vec2f, event: Event? = null) {
        val fixed = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) rotation.getRotation() else Rotation(
            rotation.x,
            rotation.y
        ).fixedSensitivity()
        if (event != null && AntiCheat.ac == AntiCheat.AntiCheats.GrimAC) {
            runSafe { doRotate(event, fixed.yaw, fixed.pitch) }
        } else if (AntiCheat.ac == AntiCheat.AntiCheats.Legit) {
            runSafe {
                player.yaw = fixed.yaw
                player.pitch = fixed.pitch
            }
        } else {
            resetTimer.reset()
            rotateYaw = fixed.yaw
            rotationPitch = fixed.pitch
        }
    }

    @JvmStatic
    fun rotationTo(rotation: Rotation, event: Event? = null) {
        val fixedRotation = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) rotation else rotation.fixedSensitivity()
        if (event != null && AntiCheat.ac == AntiCheat.AntiCheats.GrimAC) {
            runSafe { doRotate(event, fixedRotation.yaw, fixedRotation.pitch) }
        } else if (AntiCheat.ac == AntiCheat.AntiCheats.Legit) {
            runSafe {
                player.yaw = fixedRotation.yaw
                player.pitch = fixedRotation.pitch
            }
        } else {
            resetTimer.reset()
            rotateYaw = rotation.yaw
            rotationPitch = rotation.pitch
        }
    }

    @JvmStatic
    fun rotationTo(blockPos: BlockPos, side: Boolean = false, event: Event? = null) {
        runSafe {
            val fixedRotation = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) getRotationToRotation(
                blockPos.toVec3dCenter(),
                side
            ) else getFixedRotationTo(blockPos.toVec3dCenter(), side)
            if (event != null && AntiCheat.ac == AntiCheat.AntiCheats.GrimAC) {
                doRotate(event, fixedRotation.yaw, fixedRotation.pitch)
            } else if (AntiCheat.ac == AntiCheat.AntiCheats.Legit) {
                player.yaw = fixedRotation.yaw
                player.pitch = fixedRotation.pitch
            } else {
                resetTimer.reset()
                rotateYaw = fixedRotation.yaw
                rotationPitch = fixedRotation.pitch
            }
        }
    }

    @JvmStatic
    fun rotationTo(vec3d: Vec3d, side: Boolean = false, event: Event? = null) {
        runSafe {
            val fixedRotation = if (AntiCheat.ac != AntiCheat.AntiCheats.GrimAC) getRotationToRotation(
                vec3d,
                side
            ) else getFixedRotationTo(vec3d, side)
            if (event != null && AntiCheat.ac == AntiCheat.AntiCheats.GrimAC) {
                doRotate(event, fixedRotation.yaw, fixedRotation.pitch)
            } else if (AntiCheat.ac == AntiCheat.AntiCheats.Legit) {
                player.yaw = fixedRotation.yaw
                player.pitch = fixedRotation.pitch
            } else {
                resetTimer.reset()
                rotateYaw = fixedRotation.yaw
                rotationPitch = fixedRotation.pitch
            }
        }
    }

    private fun SafeClientEvent.doRotate(event: Event, yaw: Float, pitch: Float) {
        when (event) {
            is PlayerMotionEvent -> {
                event.setRotation(yaw, pitch)
            }

            is PacketEvents.Send -> {
                if (event.packet is PlayerMoveC2SPacket) {
                    event.packet.yaw = yaw
                    event.packet.pitch = pitch
                }
            }

            else -> {
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
}