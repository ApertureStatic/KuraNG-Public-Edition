package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import melon.events.player.PlayerTravelEvent
import melon.system.event.SafeClientEvent
import melon.system.event.safeEventListener
import melon.utils.MovementUtils.calcMoveYaw
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import kotlin.math.*

object ElytraFly : Module(name = "ElytraFly", langName = "鞘翅平飞", category = Category.MOVEMENT) {
    private var speedControl by fsetting("Speed", 1.81f, 0.1f, 10f)
    private var upSpeed by fsetting("UpSpeed", 1.2f, 0.1f, 5f)
    private var downSpeed by fsetting("DownSpeed", 1f, 0.1f, 5f)
    private val accelerateTime by fsetting("AccelerateTime", 0.0f, 0.0f, 20.0f)
    private val accelerateStartSpeed by isetting("StartSpeed", 100, 0, 100)
    private var boostPitchControl by isetting("BaseBoostPitch", 20, 0, 90)
    private val forwardPitch by isetting("ForwardPitch", 0, -90, 90)
    private var legacyLookBoost by bsetting("LockBoost", true)
    private var ncpStrict by bsetting("NCPStrict", false)
    private var pitchSpoof by bsetting("PitchSpoof", false)
    private val instantFlyTimer = TimerUtils()
    private var isStandingStillH = false
    private var isStandingStill = false
    private var packetPitch = 0.0f
    private var boostingTick = 0
    private var packetYaw = 0.0f
    private var speedPercentage = 0.0f
    private var elytraDurability = 0
    private var isFlying = false
    private var elytraIsEquipped = false
    private var wasInLiquid = false

    override fun onEnable() {
        speedPercentage = accelerateStartSpeed.toFloat()
        instantFlyTimer.reset()
    }

    init {
        onPacketReceive {
            if (player.isSpectator || !elytraIsEquipped || elytraDurability <= 1 || !isFlying) return@onPacketReceive
            if (it.packet is PlayerPositionLookS2CPacket) {
                it.packet.pitch = player.pitch
            }
        }

        onPacketSend { event ->
            if (player.isFallFlying && pitchSpoof) {
                when (event.packet) {
                    is PlayerMoveC2SPacket -> {
                        when (event.packet) {
                            is PlayerMoveC2SPacket.Full -> {
                                connection.sendPacket(
                                    PlayerMoveC2SPacket.PositionAndOnGround(
                                        event.packet.x,
                                        event.packet.y,
                                        event.packet.z,
                                        event.packet.onGround
                                    )
                                )
                            }

                            is PlayerMoveC2SPacket.LookAndOnGround -> {
                                event.cancel()
                            }
                        }
                    }
                }
            }
        }

        safeEventListener<PlayerTravelEvent> {
            if (player.isSpectator) return@safeEventListener
            val armorSlot = player.inventory.armor[2]
            elytraIsEquipped = armorSlot.item == Items.ELYTRA
            if (elytraIsEquipped) {
                elytraDurability = armorSlot.maxDamage - armorSlot.damage
            }
            if (player.isTouchingWater || player.isInLava) {
                wasInLiquid = true
            } else if (player.onGround || isFlying) {
                wasInLiquid = false
            }
            isFlying = player.isFallFlying
            isStandingStillH = player.input.movementForward == 0f && player.input.movementSideways == 0f
            isStandingStill = isStandingStillH && !player.input.jumping && !player.input.sneaking
            if (!isFlying || isStandingStill) speedPercentage = accelerateStartSpeed.toFloat()
            if (elytraIsEquipped && elytraDurability > 1) {
                if (!isFlying) {
                    //takeoff(it)
                    if (!player.onGround) {
                        if (instantFlyTimer.tickAndReset(150)) {
                            connection.sendPacket(
                                ClientCommandC2SPacket(
                                    player,
                                    ClientCommandC2SPacket.Mode.START_FALL_FLYING
                                )
                            )
                        }
                    }
                    return@safeEventListener
                }
                controlMode(it)
                //spoofRotation()
            }
        }
    }

    private fun SafeClientEvent.getYaw(): Double {
        val yawRad = player.calcMoveYaw()
        packetYaw = Math.toDegrees(yawRad).toFloat()
        return yawRad
    }

    private fun getSpeed(boosting: Boolean): Double {
        return when {
            boosting -> speedControl.toDouble()

            accelerateTime != 0.0f && accelerateStartSpeed != 100 -> {
                speedPercentage =
                    min(speedPercentage + (100.0f - accelerateStartSpeed) / (accelerateTime * 20.0f), 100.0f)
                val speedMultiplier = speedPercentage / 100.0

                speedControl * speedMultiplier * (cos(speedMultiplier * PI) * -0.5 + 0.5)
            }

            else -> speedControl.toDouble()
        }
    }

    private fun SafeClientEvent.setSpeed(yaw: Double, boosting: Boolean) {
        val acceleratedSpeed = getSpeed(boosting)
        player.setVelocity(sin(-yaw) * acceleratedSpeed, player.velocity.y, cos(yaw) * acceleratedSpeed)
    }

    private fun SafeClientEvent.controlMode(event: PlayerTravelEvent) {
        /* States and movement input */
        val currentSpeed = hypot(player.velocity.x, player.velocity.z)
        val moveUp = if (!legacyLookBoost) player.input.jumping else player.pitch < -10.0f && !isStandingStillH
        val moveDown = if (mc.currentScreen != null || moveUp) false else player.input.sneaking

        /* Set velocity */
        if (!isStandingStillH || moveUp) {
            if (moveUp && (currentSpeed >= 0.8 || player.velocity.y > 1.0)) {
                upwardFlight(upSpeed.toDouble(), getYaw())
            } else { /* Runs when pressing wasd */
                packetPitch = forwardPitch.toFloat()
                setSpeed(getYaw(), moveUp)
                boostingTick = 0
            }
        } else player.setVelocity(0.0, 0.0, 0.0) /* Stop moving if no inputs are pressed */

        if (moveDown) player.velocity.y = -downSpeed.toDouble() /* Runs when holding shift */

        event.cancel()
    }

    private fun SafeClientEvent.upwardFlight(currentSpeed: Double, yaw: Double) {
        val multipliedSpeed = 0.128 * speedControl
        val strictPitch =
            Math.toDegrees(asin((multipliedSpeed - sqrt(multipliedSpeed * multipliedSpeed - 0.0348)) / 0.12)).toFloat()
        val basePitch = if (ncpStrict && strictPitch < boostPitchControl && !strictPitch.isNaN()) -strictPitch
        else -boostPitchControl.toFloat()
        val targetPitch = if (player.pitch < 0.0f) {
            max(
                player.pitch * (90.0f - boostPitchControl.toFloat()) / 90.0f - boostPitchControl.toFloat(),
                -90.0f
            )
        } else -boostPitchControl.toFloat()

        packetPitch = if (packetPitch <= basePitch && boostingTick > 2) {
            if (packetPitch < targetPitch) packetPitch += 17.0f
            if (packetPitch > targetPitch) packetPitch -= 17.0f
            max(packetPitch, targetPitch)
        } else basePitch
        boostingTick++

        /* These are actually the original Minecraft elytra fly code lol */
        val pitch = Math.toRadians(packetPitch.toDouble())
        val targetMotionX = sin(-yaw) * sin(-pitch)
        val targetMotionZ = cos(yaw) * sin(-pitch)
        val targetSpeed = sqrt(targetMotionX * targetMotionX + targetMotionZ * targetMotionZ)
        val upSpeed = currentSpeed * sin(-pitch) * 0.04
        val fallSpeed = cos(pitch) * cos(pitch) * 0.06 - 0.08

        player.velocity.x -= upSpeed * targetMotionX / targetSpeed - (targetMotionX / targetSpeed * currentSpeed - player.velocity.x) * 0.1
        player.velocity.y += upSpeed * 3.2 + fallSpeed
        player.velocity.z -= upSpeed * targetMotionZ / targetSpeed - (targetMotionZ / targetSpeed * currentSpeed - player.velocity.z) * 0.1

        /* Passive motion loss */
        player.velocity.x *= 0.99
        player.velocity.y *= 0.98
        player.velocity.z *= 0.99
    }
}