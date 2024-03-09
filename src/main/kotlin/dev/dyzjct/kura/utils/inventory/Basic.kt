@file:Suppress("NOTHING_TO_INLINE") // Looks like inlining stuff here reduced the size of compiled code
package dev.dyzjct.kura.utils.inventory

import base.system.event.SafeClientEvent
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType

inline fun InventoryTask.Builder.pickUp(slot: Slot) {
    pickUp(0, slot)
}

inline fun InventoryTask.Builder.pickUp(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 0, SlotActionType.PICKUP)
}

inline fun InventoryTask.Builder.pickUpAll(slot: Slot) {
    pickUpAll(0, slot)
}

inline fun InventoryTask.Builder.pickUpAll(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 0, SlotActionType.PICKUP_ALL)
}

inline fun InventoryTask.Builder.quickMove(slot: Slot) {
    quickMove(0, slot)
}

inline fun InventoryTask.Builder.quickMove(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 0, SlotActionType.QUICK_MOVE)
}

inline fun InventoryTask.Builder.swapWith(slot: Slot, hotbarSlot: HotbarSlot) {
    swapWith(0, slot, hotbarSlot)
}


inline fun InventoryTask.Builder.swapWith(windowID: Int, slot: Slot, hotbarSlot: HotbarSlot) {
    +Click(windowID, slot, hotbarSlot.hotbarSlot, SlotActionType.SWAP)
}

inline fun InventoryTask.Builder.throwOne(slot: Slot) {
    throwOne(0, slot)
}

inline fun InventoryTask.Builder.throwOne(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 0, SlotActionType.THROW)
}

inline fun InventoryTask.Builder.throwAll(slot: Slot) {
    throwAll(0, slot)
}

inline fun InventoryTask.Builder.throwAll(windowID: Int, slot: Slot) {
    +Click(windowID, slot, 1, SlotActionType.THROW)
}

fun InventoryTask.Builder.action(block: SafeClientEvent.() -> Unit) {
    +object : Step {
        override fun run(event: SafeClientEvent): StepFuture {
            block.invoke(event)
            return InstantFuture
        }
    }
}