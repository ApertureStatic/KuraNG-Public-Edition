package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.inventory.InventoryUtil.inventoryAndHotbarSlots
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.SlotActionType
import java.util.concurrent.atomic.AtomicInteger

object AutoOffHand : Module(
    name = "AutoOffHand",
    langName = "自动副手",
    description = "auto swap item to offhand",
    category = Category.COMBAT
) {
    private val strict = bsetting("Strict", false)
    private val itemMode = msetting("Items", ItemMode.Totem)
    private var preferredSlot = 0
    private var numOfItems = 0
    private val timer = TimerUtils()

    init {
        onMotion {
            val findingItem = when (itemMode.value) {
                ItemMode.Totem -> Items.TOTEM_OF_UNDYING
                ItemMode.Gap -> Items.ENCHANTED_GOLDEN_APPLE
                else -> Items.END_CRYSTAL
            }
            if (!findItems(findingItem) || (mc.currentScreen is ScreenHandlerContext && mc.currentScreen !is InventoryScreen)) {
                return@onMotion
            }
            if (timer.tickAndReset(25)) {
                if (player.offHandStack.item != findingItem) {
                    val offhandEmptyPreSwitch = player.offHandStack.item == Items.AIR
                    legitBypass(preferredSlot)
                    legitBypass(45)
                    if (!offhandEmptyPreSwitch) {
                        legitBypass(preferredSlot)
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.legitBypass(slot: Int) {
        runCatching {
            if (strict.value) {
                player.velocity.x = 0.0
                player.velocity.z = 0.0
                player.movementSpeed = 0.0f
            }
            playerController.clickSlot(player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, player)
        }
    }

    private fun SafeClientEvent.findItems(item: Item): Boolean {
        numOfItems = 0
        val preferredSlotStackSize = AtomicInteger()
        preferredSlotStackSize.set(Int.MIN_VALUE)
        inventoryAndHotbarSlots.forEach { (slotKey: Int, slotValue: ItemStack) ->
            var numOfItemsInStack = 0
            if (slotValue.item == item) {
                numOfItemsInStack = slotValue.count
                if (preferredSlotStackSize.get() < numOfItemsInStack) {
                    preferredSlotStackSize.set(numOfItemsInStack)
                    preferredSlot = slotKey
                }
            }
            numOfItems += numOfItemsInStack
        }
        if (player.offHandStack.item == item) {
            numOfItems += player.offHandStack.count
        }
        return numOfItems != 0
    }

    @Suppress("UNUSED")
    enum class ItemMode {
        Totem, EndCrystal, Gap
    }

    override fun getHudInfo(): String {
        return numOfItems.toString() + ""
    }
}