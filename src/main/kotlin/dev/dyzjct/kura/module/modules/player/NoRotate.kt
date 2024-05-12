package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object NoRotate : Module(name = "NoRotate", langName = "防回弹转头", category = Category.PLAYER, type = Type.Both) {
    init {
        onPacketReceive { event ->
            if (event.packet is PlayerPositionLookS2CPacket) {
                event.packet.yaw = player.getYaw()
                event.packet.pitch = player.getPitch()
            }
        }
    }
}