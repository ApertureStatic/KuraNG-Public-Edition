package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import melon.events.player.PlayerMoveEvent
import melon.system.event.SafeClientEvent
import melon.system.event.safeEventListener
import melon.system.util.interfaces.DisplayEnum
import melon.utils.world.noCollision
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Box
import kotlin.math.cos
import kotlin.math.sin

object Flight : Module(
    name = "Flight",
    category = Category.MOVEMENT,
    description = ""
) {

    private val mode = msetting("Mode", Mode.Vanilla)
    private val hSpeed by dsetting("Horizontal", 1.0, 0.0, 10.0).enumIsNot(mode, Mode.MatrixJump)
    private val vSpeed by dsetting("Vertical", 1.0, 0.0, 10.0).enumIsNot(mode, Mode.MatrixJump)
    private var glide = bsetting("Glide", true).enumIsNot(mode, Mode.MatrixJump)
    private var glideSpeed by dsetting("GlideSpeed", 0.0, 0.0, 10.0, 0.01).isTrue(glide)
        .enumIsNot(mode, Mode.MatrixJump)
    private val autoToggle by bsetting("AutoToggle", false).enumIsNot(mode, Mode.MatrixJump)

    private var groundTimer = TimerUtils()
    private var prevX = 0.0
    private var prevY = 0.0
    private var prevZ = 0.0
    private var onPosLook = false
    private var flyTicks = 0


    init {
        safeEventListener<PlayerMoveEvent> {
            when (mode.value) {
                Mode.Vanilla -> {
                    player.abilities.flying = false
                    val dir = forward(hSpeed)
                    player.setVelocity(dir[0], 0.0, dir[1])
                    if (!player.collidedSoftly && glide.value) player.velocity.y -= glideSpeed
                    if (mc.options.jumpKey.isPressed) player.velocity =
                        player.velocity.add(0.0, vSpeed, 0.0)
                    if (mc.options.sneakKey.isPressed) player.velocity =
                        player.velocity.add(0.0, -vSpeed, 0.0)
                    handleVanillaKickBypass()
                }

                Mode.AirJump -> {
                    if (world.getBlockCollisions(
                            player,
                            player.boundingBox.expand(0.5, 0.0, 0.5)
                                .offset(0.0, -1.0, 0.0)
                        ).iterator().hasNext()
                    ) {
                        player.isOnGround = true
                        player.jump()
                    }
                }

                Mode.MatrixGlide -> {
                    if (player.isOnGround) {
                        player.jump()
                        flyTicks = 5
                    } else if (flyTicks > 0) {
                        val dir: DoubleArray = forward(hSpeed.toDouble())
                        player.setVelocity(dir[0], -0.04, dir[1])
                        flyTicks--
                    }
                }
            }
        }

        onLoop {
            if (mode.value != Mode.MatrixJump) return@onLoop
            player.abilities.flying = false
            player.setVelocity(0.0, 0.0, 0.0)

            if (mc.options.jumpKey.isPressed) player.velocity = player.velocity.add(0.0, vSpeed.toDouble(), 0.0)
            if (mc.options.sneakKey.isPressed) player.velocity = player.velocity.add(0.0, -vSpeed.toDouble(), 0.0)

            val dir: DoubleArray = forward(hSpeed.toDouble())
            player.setVelocity(dir[0], player.velocity.getY(), dir[1])
        }

        onPacketReceive {
            if (mode.value == Mode.MatrixJump) {
                if (it.packet is PlayerPositionLookS2CPacket) {
                    onPosLook = true
                    prevX = player.velocity.getX()
                    prevY = player.velocity.getY()
                    prevZ = player.velocity.getZ()
                }
            }
        }

        onPacketSend {
            if (mode.value == Mode.MatrixJump) {
                if (it.packet is Full) {
                    if (onPosLook) {
                        player.setVelocity(prevX, prevY, prevZ)
                        onPosLook = false
                        if (autoToggle) disable()
                    }
                }
            }

        }
    }

    private fun SafeClientEvent.handleVanillaKickBypass() {
        if (groundTimer.tickAndReset(1000)) {
            val ground = calculateGround()
            connection.sendPacket(Full(player.x, ground, player.z, player.yaw, player.pitch, true))
            connection.sendPacket(Full(player.x, player.y, player.z, player.yaw, player.pitch, true))
        }
    }

    private fun SafeClientEvent.calculateGround(): Double {
        val playerBoundingBox = player.boundingBox
        var blockHeight = 1.0

        var ground = player.y
        while (ground > 0.0) {
            val customBox = Box(
                playerBoundingBox.maxX,
                ground + blockHeight,
                playerBoundingBox.maxZ,
                playerBoundingBox.minX,
                ground,
                playerBoundingBox.minZ
            )
            if (world.noCollision(customBox)) {
                if (blockHeight <= 0.05) return ground + blockHeight
                ground += blockHeight
                blockHeight = 0.05
            }
            ground -= blockHeight
        }

        return 0.0
    }

    private fun SafeClientEvent.forward(d: Double): DoubleArray {
        var f = player.input.movementForward
        var f2 = player.input.movementSideways
        var f3 = player.yaw
        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += (if (f > 0.0f) -45 else 45).toFloat()
            } else if (f2 < 0.0f) {
                f3 += (if (f > 0.0f) 45 else -45).toFloat()
            }
            f2 = 0.0f
            if (f > 0.0f) {
                f = 1.0f
            } else if (f < 0.0f) {
                f = -1.0f
            }
        }
        val d2 = sin(Math.toRadians((f3 + 90.0f).toDouble()))
        val d3 = cos(Math.toRadians((f3 + 90.0f).toDouble()))
        val d4 = f * d * d3 + f2 * d * d2
        val d5 = f * d * d2 - f2 * d * d3
        return doubleArrayOf(d4, d5)
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        Vanilla("Vanilla"),
        MatrixJump("MatrixJump"),
        AirJump("AirJump"),
        MatrixGlide("MatrixGlide")
    }

}