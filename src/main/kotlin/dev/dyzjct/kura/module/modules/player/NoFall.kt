package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.movement.ElytraFly
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object NoFall: Module(
    name = "NoFall",
    langName = "无摔落伤害",
    description = "Prevents fall damage",
    category = Category.PLAYER
) {
    private var pauseOnElyTra = bsetting("ElytraPause", true)

    init {
        onPacketSend { event ->
            if (pauseOnElyTra.value && ElytraFly.isEnabled && player.inventory.getArmorStack(2).item == Items.ELYTRA) return@onPacketSend
            if (event.packet is PlayerMoveC2SPacket) {
                event.packet.onGround = true
            }
        }
    }
}
