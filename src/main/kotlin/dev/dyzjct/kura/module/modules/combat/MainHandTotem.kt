package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType

object MainHandTotem :
    Module(name = "MainHandTotem", langName = "主手图腾", description = "null", category = Category.COMBAT) {
    private val autoSwitch by bsetting("AutoSwitch", true)
    private val slot by isetting("Slot", 0, 0, 9)
    private val health by dsetting("SwitchHealth", 15.0, 0.0, 36.0)

    init {
        onMotion {
            if (mc.currentScreen is InventoryScreen) return@onMotion
            if (player.inventory.getStack(0).item === Items.TOTEM_OF_UNDYING) return@onMotion
            for (i in 9..34) {
                if (player.inventory.getStack(i).item === Items.TOTEM_OF_UNDYING) {
                    playerController.clickSlot(
                        player.currentScreenHandler.syncId,
                        i,
                        0,
                        SlotActionType.SWAP,
                        player
                    )
                    playerController.clickSlot(
                        player.currentScreenHandler.syncId,
                        slot,
                        0,
                        SlotActionType.SWAP,
                        player
                    )
                    break
                }
            }
        }
        onLoop {
            if (healthCheck() && autoSwitch && player.inventory.getStack(0)
                    .item === Items.TOTEM_OF_UNDYING
            ) {
                player.inventory.selectedSlot = 0
            }
        }
    }

    private fun SafeClientEvent.healthCheck(): Boolean {
        return (player.health + player.absorptionAmount).toDouble() <= health
    }
}