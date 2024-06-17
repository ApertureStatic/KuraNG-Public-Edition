package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.utils.math.LagCompensator.globalInfoPingValue
import dev.dyzjct.kura.system.render.newfont.FontRenderers
import base.utils.chat.ChatUtil
import base.utils.concurrent.threads.runSafe
import net.minecraft.client.gui.DrawContext

object PingHUD : HUDModule(
    name = "Ping",
    langName = "延迟显示",
    x = 170f,
    y = 170f,
    category = Category.HUD
) {

    override fun onRender(context: DrawContext) {
        runSafe {
            val fontColor = Colors.hudColor.value.rgb
            val privatePingValue = this@runSafe.globalInfoPingValue()
            val finalString = "Ping " + ChatUtil.SECTIONSIGN + "f" + privatePingValue
            FontRenderers.lexend.drawString(context.matrices, finalString, x + 2.0, y + 3.0, fontColor)
            width = FontRenderers.lexend.getStringWidth(finalString) + 4
        }
    }
}