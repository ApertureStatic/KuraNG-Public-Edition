package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.manager.MovementManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.system.render.newfont.FontRenderers
import base.utils.chat.ChatUtil
import base.utils.concurrent.threads.runSafe
import net.minecraft.client.gui.DrawContext
import java.text.DecimalFormat

object SpeedHUD : HUDModule(
    name = "SpeedHUD",
    langName = "移动速度",
    x = 140f,
    y = 160f,
    category = Category.HUD
) {
    private val formatter = DecimalFormat("#.#")

    override fun onRender(context: DrawContext) {
        runSafe {
            val fontColor = Colors.hudColor.value.rgb
            val finalString = "Speed " + ChatUtil.SECTIONSIGN + "f" + speed() + " km/h"
            FontRenderers.lexend.drawString(context.matrices, finalString, x + 2.0, y + 3.0, fontColor)
            width = FontRenderers.lexend.getStringWidth(finalString) + 4
        }
    }

    fun SafeClientEvent.speed(): String {
        val currentTps: Float = mc.renderTickCounter.tickTime / 1000.0f
        return formatter.format(MovementManager.currentPlayerSpeed / currentTps * 3.6)
    }
}