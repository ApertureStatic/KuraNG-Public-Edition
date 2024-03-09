package melon.utils.inventory

import dev.dyzjct.kura.mixins.IClientPlayerInteractionManager
import dev.dyzjct.kura.utils.inventory.clickSlot
import melon.system.event.SafeClientEvent
import melon.utils.concurrent.threads.runSafe
import melon.utils.inventory.slot.craftingSlots
import melon.utils.inventory.slot.firstItem
import melon.utils.inventory.slot.getSlots
import net.minecraft.block.BlockState
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.SlotActionType
import java.util.function.Predicate

object InvUtils {
    fun SafeClientEvent.removeHoldingItem() {
        if (player.inventory.isEmpty) return

        val slot = player.playerScreenHandler.getSlots(9..45).firstItem(Items.AIR)?.id // Get empty slots in inventory and offhand
            ?: player.craftingSlots.firstItem(Items.AIR)?.id // Get empty slots in crafting slot
            ?: -999 // Throw on the ground

        clickSlot(0, slot, 0, SlotActionType.PICKUP)
    }
    private val ACTION = Action()
    var previousSlot = -1

    // Predicates
    fun SafeClientEvent.testInMainHand(predicate: Predicate<ItemStack>): Boolean {
        return predicate.test(player.mainHandStack)
    }

    fun SafeClientEvent.testInMainHand(vararg items: Item?): Boolean {
        return testInMainHand { itemStack: ItemStack ->
            for (item in items) if (itemStack.isOf(item)) return@testInMainHand true
            false
        }
    }

    fun SafeClientEvent.testInOffHand(predicate: Predicate<ItemStack>): Boolean {
        return predicate.test(player.offHandStack)
    }

    fun SafeClientEvent.testInOffHand(vararg items: Item?): Boolean {
        return testInOffHand { itemStack: ItemStack ->
            for (item in items) if (itemStack.isOf(item)) return@testInOffHand true
            false
        }
    }

    fun SafeClientEvent.testInHands(predicate: Predicate<ItemStack>): Boolean {
        return testInMainHand(predicate) || testInOffHand(predicate)
    }

    fun SafeClientEvent.testInHands(vararg items: Item?): Boolean {
        return testInMainHand(*items) || testInOffHand(*items)
    }

    fun SafeClientEvent.testInHotbar(predicate: Predicate<ItemStack>): Boolean {
        if (testInHands(predicate)) return true
        for (i in SlotUtils.HOTBAR_START until SlotUtils.HOTBAR_END) {
            val stack: ItemStack = player.inventory.getStack(i)
            if (predicate.test(stack)) return true
        }
        return false
    }

    fun SafeClientEvent.testInHotbar(vararg items: Item?): Boolean {
        return testInHotbar { itemStack: ItemStack ->
            for (item in items) if (itemStack.isOf(item)) return@testInHotbar true
            false
        }
    }

    // Finding items
    fun SafeClientEvent.findEmpty(): FindItemResult {
        return find { obj: ItemStack -> obj.isEmpty }
    }

    fun SafeClientEvent.findInHotbar(vararg items: Item): FindItemResult {
        return findInHotbar { itemStack: ItemStack ->
            for (item in items) {
                if (itemStack.item === item) return@findInHotbar true
            }
            false
        }
    }

    fun SafeClientEvent.findInHotbar(isGood: Predicate<ItemStack>): FindItemResult {
        if (testInOffHand(isGood)) {
            return FindItemResult(SlotUtils.OFFHAND, player.offHandStack.count)
        }
        return if (testInMainHand(isGood)) {
            FindItemResult(player.inventory.selectedSlot, player.mainHandStack.count)
        } else find(isGood, 0, 8)
    }

    fun SafeClientEvent.find(vararg items: Item): FindItemResult {
        return find { itemStack: ItemStack ->
            for (item in items) {
                if (itemStack.item === item) return@find true
            }
            false
        }
    }

    fun SafeClientEvent.find(isGood: Predicate<ItemStack>): FindItemResult {
        return if (player == null) FindItemResult(0, 0) else find(isGood, 0, player.inventory.size())
    }

    fun SafeClientEvent.find(isGood: Predicate<ItemStack>, start: Int, end: Int): FindItemResult {
        if (player == null) return FindItemResult(0, 0)
        var slot = -1
        var count = 0
        for (i in start..end) {
            val stack: ItemStack = player.inventory.getStack(i)
            if (isGood.test(stack)) {
                if (slot == -1) slot = i
                count += stack.count
            }
        }
        return FindItemResult(slot, count)
    }

    fun SafeClientEvent.findFastestTool(state: BlockState?): FindItemResult {
        var bestScore = 1f
        var slot = -1
        for (i in 0..8) {
            val stack: ItemStack = player.inventory.getStack(i)
            if (!stack.isSuitableFor(state)) continue
            val score = stack.getMiningSpeedMultiplier(state)
            if (score > bestScore) {
                bestScore = score
                slot = i
            }
        }
        return FindItemResult(slot, 1)
    }

    // Interactions
    fun SafeClientEvent.swap(slot: Int, swapBack: Boolean): Boolean {
        if (slot == SlotUtils.OFFHAND) return true
        if (slot < 0 || slot > 8) return false
        if (swapBack && previousSlot == -1) previousSlot =
            player.inventory.selectedSlot else if (!swapBack) previousSlot = -1
        player.inventory.selectedSlot = slot
        (playerController as IClientPlayerInteractionManager).syncSelected()
        return true
    }

    fun SafeClientEvent.swapBack(): Boolean {
        if (previousSlot == -1) return false
        val return_ = swap(previousSlot, false)
        previousSlot = -1
        return return_
    }

    fun move(): Action {
        ACTION.type = SlotActionType.PICKUP
        ACTION.two = true
        return ACTION
    }

    fun click(): Action {
        ACTION.type = SlotActionType.PICKUP
        return ACTION
    }

    fun quickMove(): Action {
        ACTION.type = SlotActionType.QUICK_MOVE
        return ACTION
    }

    fun drop(): Action {
        ACTION.type = SlotActionType.THROW
        ACTION.data = 1
        return ACTION
    }

    fun dropHand() {
        runSafe {
            if (!player.currentScreenHandler.cursorStack.isEmpty) playerController.clickSlot(
                player.currentScreenHandler.syncId,
                ScreenHandler.EMPTY_SPACE_SLOT_INDEX,
                0,
                SlotActionType.PICKUP,
                player
            )
        }
    }

    class Action {
        var type: SlotActionType? = null
        var two = false
        var from = -1
        var to = -1
        var data = 0
        var isRecursive = false

        // From
        fun fromId(id: Int): Action {
            from = id
            return this
        }

        fun from(index: Int): Action {
            return fromId(SlotUtils.indexToId(index))
        }

        fun fromHotbar(i: Int): Action {
            return from(SlotUtils.HOTBAR_START + i)
        }

        fun fromOffhand(): Action {
            return from(SlotUtils.OFFHAND)
        }

        fun fromMain(i: Int): Action {
            return from(SlotUtils.MAIN_START + i)
        }

        fun fromArmor(i: Int): Action {
            return from(SlotUtils.ARMOR_START + (3 - i))
        }

        // To
        fun toId(id: Int) {
            to = id
            run()
        }

        fun to(index: Int) {
            toId(SlotUtils.indexToId(index))
        }

        fun toHotbar(i: Int) {
            to(SlotUtils.HOTBAR_START + i)
        }

        fun toOffhand() {
            to(SlotUtils.OFFHAND)
        }

        fun toMain(i: Int) {
            to(SlotUtils.MAIN_START + i)
        }

        fun toArmor(i: Int) {
            to(SlotUtils.ARMOR_START + (3 - i))
        }

        // Slot
        fun slotId(id: Int) {
            runSafe {
                to = id
                from = to
                run()
            }
        }

        fun slot(index: Int) {
            slotId(SlotUtils.indexToId(index))
        }

        fun slotHotbar(i: Int) {
            slot(SlotUtils.HOTBAR_START + i)
        }

        fun slotOffhand() {
            slot(SlotUtils.OFFHAND)
        }

        fun slotMain(i: Int) {
            slot(SlotUtils.MAIN_START + i)
        }

        fun slotArmor(i: Int) {
            slot(SlotUtils.ARMOR_START + (3 - i))
        }

        // Other
        private fun run() {
            runSafe {
                val hadEmptyCursor: Boolean = player.currentScreenHandler.cursorStack.isEmpty
                if (type != null && from != -1 && to != -1) {
                    click(from)
                    if (two) click(to)
                }
                val preType = type
                val preTwo = two
                val preFrom = from
                val preTo = to
                type = null
                two = false
                from = -1
                to = -1
                data = 0
                if (!isRecursive && hadEmptyCursor && preType == SlotActionType.PICKUP && preTwo && preFrom != -1 && preTo != -1 && !player.currentScreenHandler.cursorStack
                        .isEmpty
                ) {
                    isRecursive = true
                    click().slotId(preFrom)
                    isRecursive = false
                }
            }
        }

        private fun click(id: Int) {
            runSafe {
                playerController.clickSlot(player.currentScreenHandler.syncId, id, data, type, player)
            }
        }
    }
}
