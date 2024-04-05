package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket

object BetterEat : Module(
    name = "BetterEat",
    langName = "更好的吃",
    category = Category.MISC
) {
    val packetEat by bsetting("PacketEat", true)
    private val switchReset by bsetting("SwitchReset", false)

    init {
        onPacketSend {
            if (it.packet is UpdateSelectedSlotC2SPacket && player.isUsingItem && switchReset) {
                player.stopUsingItem()
            }
        }
    }
}