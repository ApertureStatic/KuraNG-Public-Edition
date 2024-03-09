package melon.utils.inventory.slot

import dev.dyzjct.kura.utils.inventory.HotbarSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot

inline val PlayerEntity.allSlots: List<Slot>
    get() = playerScreenHandler.getSlots(1..45)

inline val PlayerEntity.armorSlots: List<Slot>
    get() = playerScreenHandler.getSlots(5..8)

inline val PlayerEntity.headSlot: Slot
    get() = playerScreenHandler.slots[5]

inline val PlayerEntity.chestSlot: Slot
    get() = playerScreenHandler.slots[6]

inline val PlayerEntity.legsSlot: Slot
    get() = playerScreenHandler.slots[7]

inline val PlayerEntity.feetSlot: Slot
    get() = playerScreenHandler.slots[8]

inline val PlayerEntity.offhandSlot: Slot
    get() = playerScreenHandler.slots[45]

inline val PlayerEntity.craftingSlots: List<Slot>
    get() = playerScreenHandler.getSlots(1..4)

inline val PlayerEntity.playerScreenHandler: List<Slot>
    get() = playerScreenHandler.getSlots(9..44)

inline val PlayerEntity.storageSlots: List<Slot>
    get() = playerScreenHandler.getSlots(9..35)

inline val PlayerEntity.hotbarSlots: List<HotbarSlot>
    get() = ArrayList<HotbarSlot>().apply {
        for (slot in 36..44) {
            add(HotbarSlot(playerScreenHandler.slots[slot]))
        }
    }

inline val PlayerEntity.currentHotbarSlot: HotbarSlot
    get() = HotbarSlot(playerScreenHandler.getSlot(inventory.selectedSlot + 36))

inline val PlayerEntity.firstHotbarSlot: HotbarSlot
    get() = HotbarSlot(playerScreenHandler.getSlot(36))

inline fun PlayerEntity.getHotbarSlot(slot: Int): HotbarSlot {
    if (slot !in 0..8) throw IllegalArgumentException("Invalid hotbar slot: $slot")
    return HotbarSlot(playerScreenHandler.slots[slot + 36])
}

inline fun ScreenHandler.getSlots(range: IntRange): List<Slot> =
    slots.subList(range.first, range.last + 1)

