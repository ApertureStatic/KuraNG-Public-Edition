package dev.dyzjct.kura.module.modules.combat

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.inventory.InventoryUtil.inventoryAndHotbarSlots
import base.system.event.SafeClientEvent
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.math.Vec3d
import java.util.concurrent.atomic.AtomicInteger

object AutoTotem : Module(
    name = "AutoTotem",
    langName = "自动图腾",
    description = "auto swap totem to offhand",
    category = Category.COMBAT
) {
    private var strict = bsetting("Strict", false)
    private var packetListen = false
    private var preferredTotemSlot = 0
    private var numOfTotems = 0

    init {
        onMotion {
            if (!findTotems() || (mc.currentScreen is ScreenHandlerContext && mc.currentScreen !is InventoryScreen)) {
                packetListen = false
                return@onMotion
            }
            if (player.offHandStack.item != Items.TOTEM_OF_UNDYING) {
                packetListen = true
                val offhandEmptyPreSwitch = player.offHandStack.item == Items.AIR
                legitBypass(preferredTotemSlot)
                legitBypass(45)
                if (!offhandEmptyPreSwitch) {
                    legitBypass(preferredTotemSlot)
                }
            } else {
                packetListen = false
            }
        }
    }

    private fun SafeClientEvent.legitBypass(slot: Int) {
        runCatching {
            if (strict.value) {
                player.velocity = Vec3d.ZERO
            }
            playerController.clickSlot(player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, player)
        }
    }

    private fun SafeClientEvent.findTotems(): Boolean {
        numOfTotems = 0
        val preferredTotemSlotStackSize = AtomicInteger()
        preferredTotemSlotStackSize.set(Int.MIN_VALUE)
        inventoryAndHotbarSlots.forEach { (slotKey: Int, slotValue: ItemStack) ->
            var numOfTotemsInStack = 0
            if (slotValue.item == Items.TOTEM_OF_UNDYING) {
                numOfTotemsInStack = slotValue.count
                if (preferredTotemSlotStackSize.get() < numOfTotemsInStack) {
                    preferredTotemSlotStackSize.set(numOfTotemsInStack)
                    preferredTotemSlot = slotKey
                }
            }
            numOfTotems += numOfTotemsInStack
        }
        if (player.offHandStack.item == Items.TOTEM_OF_UNDYING) {
            numOfTotems += player.offHandStack.count
        }
        return numOfTotems != 0
    }

    override fun getHudInfo(): String {
        return numOfTotems.toString() + ""
    }
}