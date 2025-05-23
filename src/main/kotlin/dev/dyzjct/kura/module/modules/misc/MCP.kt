package dev.dyzjct.kura.module.modules.misc

import base.utils.screen.ScreenUtils.safeReturn
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.input.MouseClickEvent
import dev.dyzjct.kura.manager.HotbarManager
import dev.dyzjct.kura.manager.HotbarManager.spoofHotbarNoCheck
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.module.modules.combat.PearlFucker
import net.minecraft.item.Items
import net.minecraft.util.Hand

object MCP : Module(
    name = "MCP", langName = "中键扔珍珠", description = "Middle pearl", category = Category.MISC
) {

    init {
        safeEventListener<MouseClickEvent> {
            if (it.button == MouseClickEvent.MouseButton.MIDDLE && it.action == MouseClickEvent.MouseAction.PRESS) {
                if (mc.currentScreen.safeReturn()) return@safeEventListener
                HotbarManager.onlyItem = Items.ENDER_PEARL
                spoofHotbarNoCheck(Items.ENDER_PEARL) {
                    interactPearl()
                }
                HotbarManager.onlyItem = null
            }
        }
    }

    private fun SafeClientEvent.interactPearl() {
        playerController.interactItem(player, Hand.MAIN_HAND)
        PearlFucker.ignoreTimer.reset()
    }
}