package dev.dyzjct.kura.module.hud

import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import net.minecraft.client.gui.DrawContext
import java.awt.Color

object WaterMarkHUD :
    HUDModule(name = "WaterMarkHUD", langName = "标题显示", category = Category.HUD, x = 100f, y = 100f) {
    private val size by fsetting("Size", 1.0f, 0.1f, 2.0f)
    private val custom by bsetting("Custom", false)
    private val watermark by ssetting("TitleName", Kura.MOD_NAME + " Client").isTrue { custom }
    private val fonts by msetting("Fonts", FontMode.Default)
    private val rainbow by bsetting("Rainbow", false)
    private val color by csetting("Color", Color(255, 255, 255)).isFalse { rainbow }
    private val speed by isetting("Speed", 18, 2, 54)
    private val saturation by fsetting("Saturation", 0.65f, 0.0f, 1.0f).isTrue { rainbow }
    private val brightness by fsetting("Brightness", 1.0f, 0.0f, 1.0f).isTrue { rainbow }
    override fun onRender(context: DrawContext) {
        val fontColor = if (!rainbow) color.rgb else Render2DEngine.rainbow(
            speed,
            0,
            saturation,
            brightness,
            1f
        ).rgb
        val font = when (fonts) {
            FontMode.Default -> FontRenderers.big_default
            FontMode.Mono -> FontRenderers.jbMono
            FontMode.Never -> FontRenderers.never
            else -> FontRenderers.big_default
        }
        context.matrices.push()
        context.matrices.scale(size / 2f, size / 2f, 1.0f)
        context.matrices.translate((x / (size / 2f)) - x, (y / (size / 2f)) - y, 0.0f)
        font.drawString(
            context.matrices,
            if (custom) watermark else Kura.MOD_NAME + " Client",
            x + 2.0,
            y + 3.0,
            fontColor
        )
        width =
            (font.getStringWidth(if (custom) watermark else Kura.MOD_NAME + " Client") + 4) * (if (size / 2f >= 0.3f) (size / 2f) else 0.3f)
        height =
            (font.getFontHeight(if (custom) watermark else Kura.MOD_NAME + " Client") + 4) * (if (size / 2f >= 0.3f) (size / 2f) else 0.3f)
        context.matrices.pop()
    }

    enum class FontMode {
        Default, Mono, Never
    }
}