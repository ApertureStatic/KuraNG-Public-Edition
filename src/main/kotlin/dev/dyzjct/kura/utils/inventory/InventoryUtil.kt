package dev.dyzjct.kura.utils.inventory

import base.utils.concurrent.threads.runSafe
import base.utils.item.attackDamage
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.utils.extension.packetClick
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.*
import net.minecraft.potion.PotionUtil
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.math.BlockPos

object InventoryUtil {

    fun SafeClientEvent.findEmptySlots(): List<Int> {
        val outPut = ArrayList<Int>()
        for ((key, value) in inventoryAndHotbarSlots) {
            if (!value.isEmpty && value.item !== Items.AIR) continue
            outPut.add(key)
        }
        return outPut
    }

    fun SafeClientEvent.findItemInInventory(item: Item): Int? {
        runSafe {
            for (i in 0..44) {
                val stack = player.inventory.getStack(i)
                if (item == stack.item) {
                    return i
                }
            }
        }
        return null
    }

    fun SafeClientEvent.findItemInHotbar(item: Item): Int? {
        runSafe {
            for (i in 0..8) {
                val stack = player.inventory.getStack(i)
                if (item == stack.item) {
                    return i
                }
            }
        }
        return null
    }

    fun SafeClientEvent.findPotInventorySlot(potion: StatusEffect): Int? {
        for (i in 0..44) {
            val stack: ItemStack = player.inventory.getStack(i)
            if (stack == ItemStack.EMPTY || stack.item !is SplashPotionItem) {
                continue
            }
            val effects: List<StatusEffectInstance> = java.util.ArrayList(PotionUtil.getPotionEffects(stack))
            for (potionEffect in effects) {
                if (potionEffect.effectType === potion) {
                    return i
                }
            }
        }
        return null
    }

    fun SafeClientEvent.getWeaponSlot(): ItemStack? {
        var bestItem: ItemStack? = null
        for (i in 0..44) {
            if (player.inventory.getStack(i).item is SwordItem || player.inventory.getStack(i).item is ToolItem) {
                bestItem?.let {
                    if (player.inventory.getStack(i).attackDamage > it.damage) bestItem = player.inventory.getStack(i)
                } ?: run {
                    bestItem = player.inventory.getStack(i)
                }
            }
        }
        return bestItem
    }

    fun SafeClientEvent.findItemInventorySlot(item: Item): Int {
        for (i in 0..44) {
            val stack = player.inventory.getStack(i)
            if (stack.item === item) return if (i < 9) i + 36 else i
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