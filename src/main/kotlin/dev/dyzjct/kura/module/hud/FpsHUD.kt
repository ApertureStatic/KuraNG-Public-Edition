package dev.dyzjct.kura.module.hud

import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.utils.math.FrameRateCounter
import base.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Formatting

object FpsHUD : HUDModule(name = "FpsHUD", langName = "帧数显示") {
    override fun onRender(context: DrawContext) {
        val text = "FPS " + Formatting.WHITE + FrameRateCounter.fps

        val textWidth = FontRenderers.lexend.getStringWidth(text)
        width = textWidth + 4f

        FontRenderers.lexend.drawString(
                context.matrices,
                text,
                x + 2,
                y + 3,
                Colors.hudColor.value.rgb,
                false
        )
    }
}