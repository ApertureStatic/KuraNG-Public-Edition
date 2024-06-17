package dev.dyzjct.kura.utils.inventory

import com.google.common.collect.Lists
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import base.utils.concurrent.threads.onMainThreadSafe
import base.utils.inventory.slot.hotbarSlots
import base.utils.inventory.slot.swapToSlot
import base.utils.item.isTool
import base.utils.player.updateController
import net.minecraft.block.BlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType

fun SafeClientEvent.equipBestTool(blockState: BlockState) {
    findBestTool(blockState)?.let {
        swapToSlot(it)
    }
}

fun SafeClientEvent.findBestTool(blockState: BlockState): HotbarSlot? {
    var maxSpeed = 1.0f
    var bestSlot: HotbarSlot? = null

    for (slot in player.hotbarSlots) {
        val stack = slot.stack

        if (stack.isEmpty || !stack.item.isTool) {
            continue
        } else {
            var speed = stack.getMiningSpeedMultiplier(blockState)

            if (speed <= 1.0f) {
                continue
            } else {
                val efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack)
                if (efficiency > 0) {
                    speed += efficiency * efficiency + 1.0f
                }
            }

            if (speed > maxSpeed) {
                maxSpeed = speed
                bestSlot = slot
            }
        }
    }

    return bestSlot
}

/**
 * Performs inventory clicking in specific window, slot, mouseButton, and click type
 *
 * @return Transaction id
 */
fun SafeClientEvent.clickSlot(windowID: Int, slot: Slot, mouseButton: Int, type: SlotActionType): Int {
    return clickSlot(windowID, slot.id, mouseButton, type)
}

/**
 * Performs inventory clicking in specific window, slot, mouseButton, and click type
 *
 * @return Transaction id
 */
fun SafeClientEvent.clickSlot(windowID: Int, slot: Int, mouseButton: Int, type: SlotActionType): Int {
    val container = player.currentScreenHandler

    val transactionID = container.nextRevision()
    //val itemStack = container.onSlotClick(slot, mouseButton, type, player)

    val defaultedList = container.slots
    val i = defaultedList.size
    val list = Lists.newArrayListWithCapacity<ItemStack>(i)
    for (slot0 in defaultedList) {
        list.add(slot0.stack.copy())
    }
    val int2ObjectMap = Int2ObjectOpenHashMap<ItemStack>()
    for (j in 0 until i) {
        var itemStack2: ItemStack
        val itemStack0 = list[j] as ItemStack
        if (ItemStack.areEqual(itemStack0, defaultedList[j].stack.also { itemStack2 = it })) continue
        int2ObjectMap.put(j, itemStack2.copy())
    }
    connection.sendPacket(
        ClickSlotC2SPacket(
            windowID,
            transactionID,
            slot,
            mouseButton,
            type,
            container.cursorStack.copy(),
            int2ObjectMap
        )
    )
    onMainThreadSafe { playerController.updateController() }

    return transactionID
}

fun SafeClientEvent.getContainerForID(windowID: Int): ScreenHandler? =
    if (windowID == 0) player.playerScreenHandler
    else player.currentScreenHandler