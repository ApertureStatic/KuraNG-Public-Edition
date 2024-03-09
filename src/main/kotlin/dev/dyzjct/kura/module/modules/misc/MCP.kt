package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.manager.HotbarManager.spoofHotbar
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarBypass
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.inventory.HotbarSlot
import melon.events.input.MouseClickEvent
import melon.system.event.SafeClientEvent
import melon.system.event.safeEventListener
import melon.utils.chat.ChatUtil
import melon.utils.inventory.slot.allSlots
import melon.utils.inventory.slot.firstItem
import melon.utils.inventory.slot.hotbarSlots
import melon.utils.screen.ScreenUtils.safeReturn
import net.minecraft.item.Items
import net.minecraft.util.Hand

object MCP : Module(
    name = "MCP", langName = "中键扔珍珠", description = "Middle pearl", category = Category.MISC
) {
    private var spoofBypass by bsetting("SpoofBypass", false)

    init {
        safeEventListener<MouseClickEvent> {
            if (it.button == MouseClickEvent.MouseButton.MIDDLE && it.action == MouseClickEvent.MouseAction.PRESS) {
                if (mc.currentScreen.safeReturn()) return@safeEventListener
                if (mc.targetedEntity == null) {
                    val slot =
                        player.hotbarSlots.firstItem(Items.ENDER_PEARL) ?: (if (spoofBypass) player.allSlots.firstItem(
                            Items.ENDER_PEARL
                        ) else player.hotbarSlots.firstItem(Items.ENDER_PEARL))?.let { item -> HotbarSlot(item) }
                        ?: return@safeEventListener
                    if (spoofBypass) {
                        spoofHotbarBypass(slot) {
                            interactPearl()
                        }
                    } else {
                        spoofHotbar(slot) {
                            interactPearl()
                        }
                    }
                    spoofHotbarBypass(slot) {
                        RotationManager.stopRotation()
                        playerController.interactItem(player, Hand.MAIN_HAND)
                        RotationManager.startRotation()
                    }
                } else {
                    ChatUtil.sendErrorMessage("Pearls will be thrown on mobs!!! Cancel throwing!!!")
                }
            }
        }
    }

    private fun SafeClientEvent.interactPearl() {
        RotationManager.stopRotation()
        playerController.interactItem(player, Hand.MAIN_HAND)
        RotationManager.startRotation()
    }
}