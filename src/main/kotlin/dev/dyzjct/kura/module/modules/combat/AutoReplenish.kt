package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.inventory.Pair
import base.system.event.SafeClientEvent
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.SlotActionType

object AutoReplenish : Module(
    name = "AutoReplenish",
    langName = "自动补充装备",
    category = Category.COMBAT,
    description = "Refills items in your hotbar"
) {
    private val refillWhile = isetting("RefillAt", 32, 1, 64)
    private val tickDelay = isetting("TickDelay", 1, 0, 10)
    private val checkName by bsetting("CheckName", false)
    private val checkDamage by bsetting("CheckDamage", false)
    private var delayStep = 0

    init {
        onMotion {
            if (mc.currentScreen is ScreenHandlerContext && mc.currentScreen !is InventoryScreen) {
                return@onMotion
            }
            delayStep = if (delayStep < tickDelay.value) {
                delayStep++
                return@onMotion
            } else {
                0
            }
            val slots = findReplenishableHotbarSlot() ?: return@onMotion
            val inventorySlot = slots.key

            playerController.clickSlot(
                player.currentScreenHandler.syncId,
                inventorySlot,
                0,
                SlotActionType.QUICK_MOVE,
                player
            )
        }
    }

    private fun SafeClientEvent.findReplenishableHotbarSlot(): Pair<Int, Int>? {
        var returnPair: Pair<Int, Int>? = null
        for ((key, stack) in hotbar) {
            if (stack.isEmpty || stack.item == Items.AIR) {
                continue
            }
            if (!stack.isStackable) {
                continue
            }
            if (stack.count >= stack.maxCount) {
                continue
            }
            if (stack.count > refillWhile.value) {
                continue
            }
            val inventorySlot = findCompatibleInventorySlot(stack)
            if (inventorySlot == -1) {
                continue
            }
            returnPair = Pair(inventorySlot, key)
        }
        return returnPair
    }

    private fun SafeClientEvent.findCompatibleInventorySlot(hotbarStack: ItemStack): Int {
        var inventorySlot = -1
        var smallestStackSize = 999
        for ((key, inventoryStack) in inventory) {
            if (inventoryStack.isEmpty || inventoryStack.item === Items.AIR) {
                continue
            }
            if (!isCompatibleStacks(hotbarStack, inventoryStack)) {
                continue
            }
            val currentStackSize = player.inventory.getStack(key).count
            if (smallestStackSize > currentStackSize) {
                smallestStackSize = currentStackSize
                inventorySlot = key
            }
        }
        return inventorySlot
    }

    private fun isCompatibleStacks(stack1: ItemStack, stack2: ItemStack): Boolean {

        // check if not same item
        if (stack1.item != stack2.item) {
            return false
        }

        // check if not same block
        if (stack1.item is BlockItem && stack2.item is BlockItem) {
            val block1 = (stack1.item as BlockItem).block
            val block2 = (stack2.item as BlockItem).block
            if (block1.defaultState != block2.defaultState) {
                return false
            }
        }

        // check if not same names
        if (checkName && stack1.name != stack2.name) {
            return false
        }

        // check if not same damage (e.g. skulls)
        if (checkDamage && stack1.damage == stack2.damage) {
            return false
        }

        return true
    }


    private val SafeClientEvent.inventory: Map<Int, ItemStack>
        get() = getInventorySlots(9, 35)
    private val SafeClientEvent.hotbar: Map<Int, ItemStack>
        get() = getInventorySlots(36, 44)

    private fun SafeClientEvent.getInventorySlots(current: Int, last: Int): Map<Int, ItemStack> {
        val fullInventorySlots: MutableMap<Int, ItemStack> = HashMap()
        for (i in current..last) {
            fullInventorySlots[i] = player.playerScreenHandler.stacks[i]
        }
        return fullInventorySlots
    }
}
