package dev.dyzjct.kura.utils.inventory

import net.minecraft.screen.slot.Slot

class HotbarSlot(slot: Slot) : Slot(slot.inventory, slot.index, slot.x, slot.y) {
    init {
        id = slot.id
    }

    val hotbarSlot = slot.id - 36
    val allSlot = slot.id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HotbarSlot) return false

        return hotbarSlot == other.hotbarSlot
    }

    override fun hashCode(): Int {
        return hotbarSlot
    }
}