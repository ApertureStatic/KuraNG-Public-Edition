package base.utils.inventory

import base.system.event.SafeClientEvent
import net.minecraft.util.Hand

data class FindItemResult(val slot: Int, val count: Int) {
    fun found(): Boolean {
        return slot != -1
    }

    val SafeClientEvent.hand: Hand?
        get() {
            if (slot == SlotUtils.OFFHAND) return Hand.OFF_HAND else if (slot == player.inventory.selectedSlot) return Hand.MAIN_HAND
            return null
        }
    val SafeClientEvent.isMainHand: Boolean
        get() = hand == Hand.MAIN_HAND
    val SafeClientEvent.isOffhand: Boolean
        get() = hand == Hand.OFF_HAND
    val isHotbar: Boolean
        get() = slot >= SlotUtils.HOTBAR_START && slot <= SlotUtils.HOTBAR_END
    val isMain: Boolean
        get() = slot >= SlotUtils.MAIN_START && slot <= SlotUtils.MAIN_END
    val isArmor: Boolean
        get() = slot >= SlotUtils.ARMOR_START && slot <= SlotUtils.ARMOR_END
}
