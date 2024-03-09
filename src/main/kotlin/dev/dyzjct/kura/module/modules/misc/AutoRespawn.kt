package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import melon.events.screen.GuiScreenEvent
import melon.system.event.safeEventListener
import melon.utils.chat.ChatUtil
import net.minecraft.client.gui.screen.DeathScreen

object AutoRespawn : Module(
    name = "AutoRespawn",
    langName = "自动重生",
    description = "Automatic respawn",
    category = Category.MISC
) {

    init {
        safeEventListener<GuiScreenEvent.Display> {
            if (it.screen is DeathScreen) {
                val pos = player.blockPos
                ChatUtil.sendMessage("You died at x ${pos.x} y ${pos.y} z ${pos.z}")
                player.requestRespawn()
                mc.setScreen(null)
            }
        }
    }
}