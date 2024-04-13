package dev.dyzjct.kura.manager

import base.events.PacketEvents
import base.events.player.PlayerMotionEvent
import base.system.event.AlwaysListening
import base.system.event.SafeClientEvent
import base.system.event.safeEventListener
import base.utils.TickTimer
import base.utils.inventory.slot.allSlots
import base.utils.inventory.slot.firstItem
import base.utils.inventory.slot.hotbarSlots
import base.utils.inventory.slot.offhandSlot
import base.utils.player.updateController
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.player.PacketMine
import dev.dyzjct.kura.utils.inventory.*
import dev.dyzjct.kura.utils.inventory.InventoryUtil.findItemInInventory
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket
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

    inline fun SafeClientEvent.swapSpoof(slot: HotbarSlot, crossinline block: () -> Unit) {
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

    inline fun SafeClientEvent.pickUpSpoof(slot: HotbarSlot, crossinline block: () -> Unit) {
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
        connection.sendPacket(UpdateSelectedSlotC2SPacket(slot))
    }

    inline fun SafeClientEvent.spoofHotbar(slot: HotbarSlot, crossinline block: () -> Unit) {
        synchronized(HotbarManager) {
            spoofHotbar(slot)
            block.invoke()
            resetHotbar()
        }
    }

    inline fun SafeClientEvent.spoofHotbarWithSetting(
        item: Item,
        isCheck: Boolean = false,
        crossinline block: () -> Unit
    ): Boolean {
        if (CombatSystem.eating && player.isUsingItem) return false
        if (PacketMine.isEnabled && PacketMine.doubleBreak && PacketMine.onDoubleBreak) return false
        var notNullSlot = false
        when (CombatSystem.mode.value) {
            CombatSystem.SpoofMode.Normal -> {
                player.hotbarSlots.firstItem(item)?.let { slot ->
                    notNullSlot = true
                    synchronized(HotbarManager) {
                        if (!isCheck) {
                            spoofHotbar(slot)
                            block.invoke()
                            resetHotbar()
                        }
                    }
                }
            }

            CombatSystem.SpoofMode.Swap -> {
                player.allSlots.firstItem(item)
                    ?.let { thisItem -> HotbarSlot(thisItem) }?.let { slot ->
                        notNullSlot = true
                        val swap = slot.hotbarSlot != serverSideHotbar
                        if (!isCheck) {
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
            }

            CombatSystem.SpoofMode.China -> {
                findItemInInventory(item)?.let { slot ->
                    notNullSlot = true
                    val swap = slot != serverSideHotbar
                    if (!isCheck) {
                        if (swap) {
                            val old = player.inventory.selectedSlot
                            inventorySwap(slot)
                            block.invoke()
                            inventorySwap(slot)
                            doSwap(old)
                        } else {
                            block.invoke()
                        }
                    }
                }
            }
        }
        return notNullSlot
    }

    inline fun SafeClientEvent.spoofHotbarNoAnyCheck(
        item: Item,
        isCheck: Boolean = false,
        crossinline block: () -> Unit
    ): Boolean {
        var notNullSlot = false
        when (CombatSystem.mode.value) {
            CombatSystem.SpoofMode.Normal -> {
                player.hotbarSlots.firstItem(item)?.let { slot ->
                    notNullSlot = true
                    synchronized(HotbarManager) {
                        if (!isCheck) {
                            spoofHotbar(slot)
                            block.invoke()
                            resetHotbar()
                        }
                    }
                }
            }

            CombatSystem.SpoofMode.Swap -> {
                player.allSlots.firstItem(item)
                    ?.let { thisItem -> HotbarSlot(thisItem) }?.let { slot ->
                        notNullSlot = true
                        val swap = slot.hotbarSlot != serverSideHotbar
                        if (!isCheck) {
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
            }

            CombatSystem.SpoofMode.China -> {
                findItemInInventory(item)?.let { slot ->
                    notNullSlot = true
                    val swap = slot != serverSideHotbar
                    if (!isCheck) {
                        if (swap) {
                            val old = player.inventory.selectedSlot
                            inventorySwap(slot)
                            block.invoke()
                            inventorySwap(slot)
                            doSwap(old)
                        } else {
                            block.invoke()
                        }
                    }
                }
            }
        }
        return notNullSlot
    }

    fun SafeClientEvent.inventorySwap(slot: Int) {
        if (slot < 9) {
            spoofHotbar(slot)
            return
        }
        connection.sendPacket(PickFromInventoryC2SPacket(slot))
    }

    fun SafeClientEvent.doSwap(slot: Int) {
        player.inventory.selectedSlot = slot
        connection.sendPacket(UpdateSelectedSlotC2SPacket(slot))
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
            spoofHotbar(slot)
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
