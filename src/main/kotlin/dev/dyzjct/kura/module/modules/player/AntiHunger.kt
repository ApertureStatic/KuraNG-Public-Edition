package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.combat.Burrow
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object AntiHunger :
    Module(
        name = "AntiHunger",
        langName = "防饥饿",
        category = Category.PLAYER,
        description = "Anti Hunger."
    ) {
    private val noSprint by bsetting("NoSprint", false)

    init {
        onPacketSend {
            if (it.packet is ClientCommandC2SPacket && noSprint) {
                if (it.packet.mode == ClientCommandC2SPacket.Mode.START_SPRINTING) it.cancel()
            }
            if (it.packet is PlayerMoveC2SPacket && player.fallDistance < 3.0 && !playerController.isBreakingBlock && Burrow.isDisabled) {
                it.packet.onGround = false
            }
        }
    }
}