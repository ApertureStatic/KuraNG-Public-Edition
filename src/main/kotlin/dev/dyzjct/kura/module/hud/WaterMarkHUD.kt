package dev.dyzjct.kura.module.hud

import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.module.modules.client.UiSetting.Theme
import dev.dyzjct.kura.setting.Setting
import net.minecraft.client.gui.DrawContext

object WaterMarkHUD :
    HUDModule(name = "WaterMarkHUD", langName = "标题显示", category = Category.HUD, x = 100f, y = 100f) {
    private val size by fsetting("Size", 1.0f, 0.1f, 2.0f)
    private var watermark: Setting<String> = addStringSetting("TitleName", Kura.MOD_NAME+".dev")
    private val rainbow by bsetting("Rainbow", false)
    private val speed by isetting("Speed", 18, 2, 54)
    private val saturation by fsetting("Saturation", 0.65f, 0.0f, 1.0f).isTrue { rainbow }
    private val brightness by fsetting("Brightness", 1.0f, 0.0f, 1.0f).isTrue { rainbow }
    override fun onRender(context: DrawContext) {
        val fontColor = if (!rainbow) Colors.hudColor.value.rgb else Render2DEngine.rainbow(
            speed,
            0,
            saturation,
            brightness,
            1f
        ).rgb
        val font = FontRenderers.never
        context.matrices.push()
        context.matrices.scale(size / 2f, size / 2f, 1.0f)
        context.matrices.translate((x / (size / 2f)) - x, (y / (size / 2f)) - y, 0.0f)
        font.drawString(context.matrices, watermark.value, x + 2.0, y + 3.0, fontColor)
        width = (font.getStringWidth(watermark.value) + 4) * (if (size / 2f >= 0.3f) (size / 2f) else 0.3f)
        height = (font.getFontHeight(watermark.value) + 4) * (if (size / 2f >= 0.3f) (size / 2f) else 0.3f)
        context.matrices.pop()
    }
}