package dev.dyzjct.kura.module.modules.movement

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.utils.extension.sendSequencedPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand


object NoSlowDown : Module(
    name = "NoSlowDown",
    langName = "取消减速",
    category = Category.MOVEMENT,
    description = "Prevents being slowed down when using an item or going through cobwebs"
) {
    private var bypass = msetting("Bypass", BypassMode.Normal)
    var piston by bsetting("Piston", false)

    init {
        onMotion {
            if (player.isUsingItem) {
                if (!player.isRiding && !player.isSneaking) {
                    when (bypass.value) {
                        BypassMode.Strict -> {
                            connection.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                        }

                        BypassMode.Grim -> {
                            if (player.getActiveHand() === Hand.OFF_HAND) {
                                connection.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot % 8 + 1))
                                connection.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                            } else {
                                sendSequencedPacket(world) { id ->
                                    PlayerInteractItemC2SPacket(
                                        Hand.OFF_HAND,
                                        id
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    enum class BypassMode {
        Normal, Strict, Grim
    }
}