package dev.dyzjct.kura.manager

import dev.dyzjct.kura.utils.inventory.*
import base.events.PacketEvents
import base.events.player.PlayerMotionEvent
import base.system.event.AlwaysListening
import base.system.event.SafeClientEvent
import base.system.event.safeEventListener
import base.utils.TickTimer
import base.utils.inventory.slot.allSlots
import base.utils.inventory.slot.hotbarSlots
import base.utils.inventory.slot.offhandSlot
import base.utils.player.updateController
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.screen.slot.Slot

@Suppress("NOTHING_TO_INLINE")
object HotbarManager : AlwaysListening {
    var serverSideHotbar = 0; private set
    var swapTime = 0L; private set

    private val tick = TickTimer()

    val ClientPlayerEntity.serverSideItem: ItemStack
        get() = inventory.main[serverSideHotbar]

    fun onInit() {
        safeEventListener<PacketEvents.Send>(Int.MIN_VALUE) {
            if (it.cancelled) return@safeEventListener

            when (it.packet) {
                is UpdateSelectedSlotC2SPacket -> {
                    synchronized(HotbarManager) {
                        serverSideHotbar = it.packet.selectedSlot
                        swapTime = System.currentTimeMillis()
                    }
                }
            }
        }

        safeEventListener<PlayerMotionEvent> {
            if (tick.tickAndReset(5000)) {
                playerController.updateController()
            }
        }
    }

    inline fun SafeClientEvent.spoofHotbarBypass(slot: HotbarSlot, crossinline block: () -> Unit) {
        synchronized(HotbarManager) {
            val swap = slot.hotbarSlot != serverSideHotbar
            if (swap) {
                inventoryTaskNow {
                    val hotbarSlot = player.hotbarSlots[serverSideHotbar]
                    swapWith(slot, hotbarSlot)
                    action { block.invoke() }
                    swapWith(slot, hotbarSlot)
                }
            } else {
                block.invoke()
            }
        }
    }

    inline fun SafeClientEvent.spoofInventory(slot: HotbarSlot, crossinline block: () -> Unit) {
        synchronized(HotbarManager) {
            inventoryTaskNow {
                pickUp(slot)
                action { block.invoke() }
                pickUp(slot)
            }
        }
    }

    inline fun SafeClientEvent.spoofOffhand(slot: Slot, crossinline block: () -> Unit) {
        synchronized(HotbarManager) {
            inventoryTaskNow {
                val offhand = player.offhandSlot
                pickUp(slot)
                pickUp(offhand)
                action { block.invoke() }
                pickUp(slot)
                pickUp(offhand)
            }
        }
    }

    inline fun SafeClientEvent.moveInv(slot: Int, crossinline block: () -> Unit) {
        synchronized(HotbarManager) {
            inventoryTaskNow {
                quickMove(player.allSlots[slot])
                block.invoke()
            }
        }
    }

    inline fun SafeClientEvent.spoofHotbar(slot: HotbarSlot) {
        return spoofHotbar(slot.hotbarSlot)
    }

    inline fun SafeClientEvent.spoofHotbar(slot: Int) {
        if (serverSideHotbar != slot && slot >= 0) {
            connection.sendPacket(UpdateSelectedSlotC2SPacket(slot))
        }
    }

    inline fun SafeClientEvent.spoofHotbar(slot: HotbarSlot, crossinline block: () -> Unit) {
        synchronized(HotbarManager) {
            spoofHotbar(slot)
            block.invoke()
            resetHotbar()
        }
    }

    inline fun SafeClientEvent.spoofHotbar(slot: Int, crossinline block: () -> Unit) {
        synchronized(HotbarManager) {
            spoofHotbar(slot)
            block.invoke()
            resetHotbar()
        }
    }

    inline fun SafeClientEvent.resetHotbar() {
        synchronized(HotbarManager) {
            val slot = playerController.lastSelectedSlot
            if (serverSideHotbar != slot) {
                spoofHotbar(slot)
            }
        }
    }

    inline fun SafeClientEvent.bypassTo(slot: HotbarSlot) {
        synchronized(HotbarManager) {
            inventoryTaskNow {
                swapWith(slot, player.hotbarSlots[player.inventory.selectedSlot])
            }
        }
    }
}
