package base.utils.inventory.slot

import dev.dyzjct.kura.utils.inventory.HotbarSlot
import base.utils.Wrapper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.Slot
import java.util.function.Predicate

/**
 * Find an empty slot or slot that matches [predicate]
 * or slot 0 if none of those were found
 */
fun PlayerEntity.anyHotbarSlot(predicate: Predicate<ItemStack>? = null): HotbarSlot {
    val hotbarSlots = this.hotbarSlots
    return hotbarSlots.firstEmpty()
        ?: hotbarSlots.firstByStack(predicate)
        ?: this.firstHotbarSlot
}

/**
 * Find an empty slot or slot 0
 */
fun PlayerEntity.anyHotbarSlot() =
    this.hotbarSlots.firstEmpty()
        ?: this.firstHotbarSlot


fun Slot.isHotbarSlot(): Boolean {
    return this.index in 36..44 && this.inventory == Wrapper.player?.inventory
}

fun Slot.toHotbarSlotOrNull() =
    if (isHotbarSlot()) HotbarSlot(this)
    else null