package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import kotlin.math.cos
import kotlin.math.sin

object ControlElytraFly: Module(
    name = "ControlElytraFly",
    langName = "鞘翅平飞+",
    category = Category.PLAYER
) {

    private val speed by dsetting("Speed", 0.5, 0.1, 4.0)
    private val takeoff by bsetting("TakeOff", true)

    init {
        onMotion {
            val yaw = Math.toRadians(player.yaw.toDouble())

            if (!player.isFallFlying) {
                if (takeoff && !player.isOnGround && mc.options.jumpKey.isPressed) {
                    connection.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING))
                }
                return@onMotion
            }

            if (player.abilities.flying) {
                player.abilities.flying = false
            }

            if (mc.options.forwardKey.isPressed) {
                player.addVelocity(-sin(yaw) * speed / 10, 0.0, cos(yaw) * speed / 10)
            }
        }
    }

}