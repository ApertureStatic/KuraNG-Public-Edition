package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import melon.events.TickEvent
import melon.system.event.SafeClientEvent
import melon.system.event.safeEventListener
import net.minecraft.screen.slot.SlotActionType

object HotbarSwapper: Module(
    name = "HotbarSwapper",
    langName = "双物品栏",
    category = Category.PLAYER,
    description = "Double hotbar"
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