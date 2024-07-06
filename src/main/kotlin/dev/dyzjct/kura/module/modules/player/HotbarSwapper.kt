package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.event.events.TickEvent
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import net.minecraft.screen.slot.SlotActionType

object HotbarSwapper: Module(
    name = "HotbarSwapper",
    langName = "双物品栏",
    description = "Double hotbar",
    category = Category.PLAYER
) {

    private val inventoryRaw by isetting("InventoryRaw", 1, 1, 3)
    private val swapSlots by isetting("SwapSlots", 9, 1, 9)

    init {

        safeEventListener<TickEvent.Post> {
            swapStacks()
            disable()
        }

    }

    private fun SafeClientEvent.swapStacks() {
        val raw = inventoryRaw * 9

        for (i in 0 until swapSlots) {
            if (player.inventory.getStack(i) != player.inventory.getStack(raw + i)) {
                playerController.clickSlot(player.playerScreenHandler.syncId, raw + i, i, SlotActionType.SWAP, player)
            }
        }
    }

}