package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.gui.rewrite.gui.render.DrawDelegate
import dev.dyzjct.kura.gui.rewrite.gui.render.DrawScope
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.Colors
import melon.system.render.newfont.FontRenderers
import melon.utils.chat.ChatUtil.SECTIONSIGN
import net.minecraft.client.gui.DrawContext

object RamHUD : HUDModule(name = "Ram", langName = "内存显示", x = 160f, y = 160f, category = Category.HUD) {
    override fun onRender(context: DrawContext) {
        val fontColor = Colors.hudColor.value.rgb
        val finalString = "Ram Usage ${SECTIONSIGN}f${Runtime.getRuntime().freeMemory() / 1000000}"
        FontRenderers.lexend.drawString(context.matrices, finalString, x + 2.0, y + 3.0, fontColor)
        width = FontRenderers.lexend.getStringWidth(finalString) + 4
    }
}