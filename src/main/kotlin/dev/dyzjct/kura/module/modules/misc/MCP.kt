package dev.dyzjct.kura.module.modules.misc

import base.events.input.MouseClickEvent
import base.system.event.SafeClientEvent
import base.system.event.safeEventListener
import base.utils.screen.ScreenUtils.safeReturn
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarNoAnyCheck
import dev.dyzjct.kura.manager.RotationManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.item.Items
import net.minecraft.util.Hand

object MCP : Module(
    name = "MCP", langName = "中键扔珍珠", description = "Middle pearl", category = Category.MISC
) {

    init {
        safeEventListener<MouseClickEvent> {
            if (it.button == MouseClickEvent.MouseButton.MIDDLE && it.action == MouseClickEvent.MouseAction.PRESS) {
                if (mc.currentScreen.safeReturn()) return@safeEventListener
                spoofHotbarNoAnyCheck(Items.ENDER_PEARL) {
                    interactPearl()
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