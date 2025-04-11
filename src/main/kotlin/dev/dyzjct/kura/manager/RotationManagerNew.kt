package dev.dyzjct.kura.manager

import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.MovementPacketsEvent
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.RotateEvent
import dev.dyzjct.kura.event.events.input.MouseUpdateEvent
import dev.dyzjct.kura.event.events.player.LookAtEvent
import dev.dyzjct.kura.event.events.player.UpdateWalkingPlayerEvent
import dev.dyzjct.kura.module.modules.client.AntiCheat
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.math.MathUtil
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.network.packet.s2c.play.PositionFlag
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.*

object RotationManagerNew : AlwaysListening {
    var nextYaw: Float = 0f
    var nextPitch: Float = 0f
    var rotationYaw: Float = 0f
    var rotationPitch: Float = 0f
    var lastYaw: Float = 0f
    var lastPitch: Float = 0f
    val ROTATE_TIMER = TimerUtils()
    var directionVec: Vec3d? = null
    var lastGround: Boolean = false

    @JvmStatic
    var renderPitch: Float = 0f
    @JvmStatic
    var renderYawOffset: Float = 0f
    @JvmStatic
    var prevPitch: Float = 0f
    @JvmStatic
    var prevRenderYawOffset: Float = 0f
    @JvmStatic
    var prevRotationYawHead: Float = 0f
    @JvmStatic
    var rotationYawHead: Float = 0f

    fun SafeClientEvent.snapBack() {
        connection.sendPacket(
            PlayerMoveC2SPacket.Full(
                player.x,
                player.y,
                player.z,
                rotationYaw,
                rotationPitch,
                player.isOnGround
            )
        )
    }

    fun SafeClientEvent.lookAt(directionVec: Vec3d) {
        rotationTo(directionVec)
        snapAt(directionVec)
    }

    fun SafeClientEvent.lookAt(pos: BlockPos, side: Direction) {
        val hitVec = pos.toCenterPos().add(Vec3d(side.vector.x * 0.5, side.vector.y * 0.5, side.vector.z * 0.5))
        lookAt(hitVec)
    }

    fun SafeClientEvent.snapAt(yaw: Float, pitch: Float) {
        setRenderRotation(yaw, pitch, true)
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
        val angle = getRotation(directionVec)
        if (AntiCheat.no_spam) {
            if (MathHelper.angleBetween(
                    angle[0],
                    lastYaw
                ) < AntiCheat.fov && abs(
                    angle[1] - lastPitch
                ) < AntiCheat.fov
            ) {
                return
            }
        }
        snapAt(angle[0], angle[1])
    }

    fun SafeClientEvent.getRotation(eyesPos: Vec3d, vec: Vec3d): FloatArray {
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

    fun rotationTo(vec3d: Vec3d?) {
        ROTATE_TIMER.reset()
        directionVec = vec3d
    }

    fun SafeClientEvent.inFov(directionVec: Vec3d, fov: Float): Boolean {
        val angle = getRotation(
            Vec3d(
                player.x,
                player.y + player.getEyeHeight(player.pose),
                player.z
            ), directionVec
        )
        return inFov(angle[0], angle[1], fov)
    }

    fun inFov(yaw: Float, pitch: Float, fov: Float): Boolean {
        return MathHelper.angleBetween(yaw, rotationYaw) + abs((pitch - rotationPitch).toDouble()) <= fov
    }

    fun onInit() {
        safeEventListener<MovementPacketsEvent> { event ->
            if (AntiCheat.moveFix == AntiCheat.MoveFix.GrimAC) {
                event.yaw = nextYaw
                event.pitch = nextPitch
            } else {
                val event1 = RotateEvent(event.yaw, event.pitch)
                event1.post()
                event.yaw = event1.yawVal
                event.pitch = event1.pitchVal
            }
        }

        safeEventListener<UpdateWalkingPlayerEvent.Post>(Int.MIN_VALUE) {
            if (AntiCheat.moveFix == AntiCheat.MoveFix.GrimAC && AntiCheat.updateMode == AntiCheat.UpdateMode.UpdateMouse) {
                updateNext()
            }
        }

        safeEventListener<MouseUpdateEvent>(Int.MIN_VALUE) {
            if (AntiCheat.moveFix == AntiCheat.MoveFix.GrimAC && AntiCheat.updateMode == AntiCheat.UpdateMode.MovementPacket) {
                updateNext()
            }
        }

        safeEventListener<RotateEvent>(Int.MIN_VALUE) { event ->
            val lookAtEvent = LookAtEvent()
            lookAtEvent.post()
            if (lookAtEvent.rotation) {
                val newAngle =
                    injectStep(floatArrayOf(lookAtEvent.yaw!!, lookAtEvent.pitch!!), lookAtEvent.speed!!)
                event.setYaw(newAngle[0])
                event.setPitch(newAngle[1])
            } else if (lookAtEvent.target != null) {
                val newAngle: FloatArray = injectStep(lookAtEvent.target!!, lookAtEvent.speed!!)
                event.setYaw(newAngle[0])
                event.setPitch(newAngle[1])
            } else if (!event.modified && AntiCheat.look) {
                if (directionVec != null && !ROTATE_TIMER.passed((AntiCheat.look_time * 1000).toLong())) {
                    val newAngle = injectStep(directionVec!!, AntiCheat.steps)
                    event.setYaw(newAngle[0])
                    event.setPitch(newAngle[1])
                }
            }
        }

        safeEventListener<PacketEvents.Send> { event ->
            if (event.cancelled) return@safeEventListener
            if (event.packet is PlayerMoveC2SPacket) {
                if (event.packet.changesLook()) {
                    lastYaw = event.packet.getYaw(lastYaw)
                    lastPitch = event.packet.getPitch(lastPitch)
                    setRenderRotation(lastYaw, lastPitch, false)
                }
                lastGround = event.packet.isOnGround
            }
        }

        safeEventListener<PacketEvents.Receive> { event ->
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
                setRenderRotation(lastYaw, lastPitch, true)
            }
        }

        safeEventListener<UpdateWalkingPlayerEvent.Post> {
            setRenderRotation(lastYaw, lastPitch, false)
        }
    }

    private fun SafeClientEvent.updateNext() {
        val rotateEvent = RotateEvent(player.getYaw(), player.getPitch())
        rotateEvent.post()
        if (rotateEvent.modified) {
            nextYaw = rotateEvent.yawVal
            nextPitch = rotateEvent.pitchVal
        } else {
            val newAngle = injectStep(
                floatArrayOf(rotateEvent.yawVal, rotateEvent.pitchVal),
                AntiCheat.steps
            )
            nextYaw = newAngle[0]
            nextPitch = newAngle[1]
        }
        AntiCheat.fixRotation = nextYaw
        AntiCheat.fixPitch = nextPitch
    }

    fun SafeClientEvent.injectStep(vec: Vec3d, steps: Float): FloatArray {
        val currentYaw = if (AntiCheat.forceSync) lastYaw else rotationYaw
        val currentPitch = if (AntiCheat.forceSync) lastPitch else rotationPitch

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
                vec.y - (player.getPos().y + player.getEyeHeight(player.pose)),
                sqrt((vec.x - player.x).pow(2.0) + (vec.z - player.z).pow(2.0))
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

    fun injectStep(angle: FloatArray?, steps: Float): FloatArray {
        var steps = steps
        if (steps < 0.01f) steps = 0.01f
        if (steps > 1) steps = 1f
        if (steps < 1 && angle != null) {
            val packetYaw = if (AntiCheat.forceSync) lastYaw else rotationYaw
            var diff = MathHelper.angleBetween(angle[0], packetYaw)
            if (abs(diff.toDouble()) > 180 * steps) {
                angle[0] = ((packetYaw + (diff * ((180 * steps) / abs(diff.toDouble())))).toFloat())
            }
            val packetPitch = if (AntiCheat.forceSync) lastPitch else rotationPitch
            diff = angle[1] - packetPitch
            if (abs(diff.toDouble()) > 90 * steps) {
                angle[1] = ((packetPitch + (diff * ((90 * steps) / abs(diff.toDouble())))).toFloat())
            }
        }
        return floatArrayOf(angle!![0], angle[1])
    }

    private var ticksExisted = 0

    fun SafeClientEvent.setRenderRotation(yaw: Float, pitch: Float, force: Boolean) {
        if (player.age == ticksExisted && !force) {
            return
        }

        ticksExisted = player.age
        prevPitch = renderPitch

        prevRenderYawOffset = renderYawOffset
        renderYawOffset = getRenderYawOffset(
            yaw,
            prevRenderYawOffset
        )

        prevRotationYawHead = rotationYawHead
        rotationYawHead = yaw

        renderPitch = pitch
    }

    private fun SafeClientEvent.getRenderYawOffset(yaw: Float, offsetIn: Float): Float {
        var result = offsetIn
        var offset: Float

        val xDif: Double = player.x - player.prevX
        val zDif: Double = player.z - player.prevZ

        if (xDif * xDif + zDif * zDif > 0.0025000002f) {
            offset = MathHelper.atan2(zDif, xDif).toFloat() * 57.295776f - 90.0f
            val wrap = MathHelper.abs(MathHelper.wrapDegrees(yaw) - offset)
            result = if (95.0f < wrap && wrap < 265.0f) {
                offset - 180.0f
            } else {
                offset
            }
        }

        if (player.handSwingProgress > 0.0f) {
            result = yaw
        }

        result = offsetIn + MathHelper.wrapDegrees(result - offsetIn) * 0.3f
        offset = MathHelper.wrapDegrees(yaw - result)

        if (offset < -75.0f) {
            offset = -75.0f
        } else if (offset >= 75.0f) {
            offset = 75.0f
        }

        result = yaw - offset
        if (offset * offset > 2500.0f) {
            result += offset * 0.2f
        }

        return result
    }
}