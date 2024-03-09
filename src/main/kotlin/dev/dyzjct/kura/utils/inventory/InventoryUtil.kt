package dev.dyzjct.kura.utils.inventory

import base.system.event.SafeClientEvent
import base.utils.concurrent.threads.runSafe
import base.utils.extension.packetClick
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.math.BlockPos
import java.util.function.Predicate

object InventoryUtil {

    fun SafeClientEvent.findEmptySlots(): List<Int> {
        val outPut = ArrayList<Int>()
        for ((key, value) in inventoryAndHotbarSlots) {
            if (!value.isEmpty && value.item !== Items.AIR) continue
            outPut.add(key)
        }
        return outPut
    }

    fun findInInventory(
        condition: Predicate<ItemStack>,
    ): Int {
        runSafe {
            for (i in 9..44) {
                val stack = player.inventory.getStack(i)
                if (condition.test(stack)) {
                    return i
                }
            }
        }
        return -1
    }

    val SafeClientEvent.inventoryAndHotbarSlots: Map<Int, ItemStack>
        get() = getInventorySlots(9)

    fun SafeClientEvent.getInventorySlots(current: Int): Map<Int, ItemStack> {
        var currentSlot = current
        val fullInventorySlots: MutableMap<Int, ItemStack> = HashMap()
        while (currentSlot <= 44) {
            fullInventorySlots[currentSlot] = player.inventory.getStack(currentSlot)
            currentSlot++
        }
        return fullInventorySlots
    }

    fun SafeClientEvent.findBestSlot(pos: BlockPos): Int {
        var slot = -1
        for (i in 0 until 9) {
            if (slot == -1 || player.inventory.getStack(i)
                    .getMiningSpeedMultiplier(world.getBlockState(pos)) > player.inventory.getStack(
                    slot
                ).getMiningSpeedMultiplier(
                    world.getBlockState(pos)
                )
            ) {
                slot = i
            }
        }
        return slot
    }

    fun SafeClientEvent.findBestItem(pos: BlockPos, inventory: Boolean): Item? {
        var slot = -1
        var item: Item? = null
        for (i in 0 until if (inventory) 46 else 9) {
            if (slot == -1 || player.inventory.getStack(i)
                    .getMiningSpeedMultiplier(world.getBlockState(pos)) > player.inventory.getStack(
                    slot
                ).getMiningSpeedMultiplier(
                    world.getBlockState(pos)
                )
            ) {
                slot = i
                item = player.inventory.getStack(i).item
            }
        }
        return item
    }

    class Task {
        private val slot: Int
        private val quickClick: Boolean
        private val packetClick: Boolean

        constructor() {
            slot = -1
            quickClick = false
            packetClick = false
        }

        constructor(slot: Int, packetClick: Boolean = false) {
            this.slot = slot
            quickClick = false
            this.packetClick = packetClick
        }

        constructor(slot: Int, quickClick: Boolean, packetClick: Boolean = false) {
            this.slot = slot
            this.quickClick = quickClick
            this.packetClick = packetClick
        }

        fun runTask() {
            runSafe {
                if (slot != -1) {
                    if (packetClick) {
                        connection.sendPacket(
                            packetClick(
                                slot, if (quickClick) SlotActionType.QUICK_MOVE else SlotActionType.PICKUP
                            )
                        )
                    } else {
                        playerController.clickSlot(
                            player.currentScreenHandler.syncId,
                            slot,
                            0,
                            if (quickClick) SlotActionType.QUICK_MOVE else SlotActionType.PICKUP,
                            player
                        )
                        playerController.clickSlot(
                            player.currentScreenHandler.syncId,
                            slot,
                            0,
                            if (quickClick) SlotActionType.QUICK_MOVE else SlotActionType.PICKUP,
                            player
                        )
                    }
                }
            }
        }
    }
}