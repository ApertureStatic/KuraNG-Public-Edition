package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.gui.rewrite.gui.render.DrawDelegate
import dev.dyzjct.kura.gui.rewrite.gui.render.DrawScope
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.Colors
import melon.system.render.newfont.FontRenderers
import melon.utils.concurrent.threads.runSafe
import net.minecraft.client.gui.DrawContext

object ServerHUD : HUDModule(
    name = "Server",
    langName = "服务器显示",
    x = 160f,
    y = 160f,
    category = Category.HUD
) {
    override fun onRender(context: DrawContext) {
        runSafe {
            val fontColor = Colors.hudColor.value.rgb
            val final =
                "IP " + "\u00a7f" + if (mc.isInSingleplayer) "Single Player" else mc.currentServerEntry?.address
            FontRenderers.lexend.drawString(context.matrices, final, x + 2.0, y + 3.0, fontColor)
            width = FontRenderers.lexend.getStringWidth(final) + 4
        }
    }
}