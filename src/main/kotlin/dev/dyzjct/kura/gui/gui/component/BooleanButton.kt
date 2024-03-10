package dev.dyzjct.kura.gui.gui.component

import dev.dyzjct.kura.gui.gui.Panel
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.setting.BooleanSetting
import dev.dyzjct.kura.utils.animations.MathUtils
import dev.dyzjct.kura.utils.math.FrameRateCounter
import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import net.minecraft.client.gui.DrawContext
import java.awt.Color

class BooleanButton(value: BooleanSetting, width: Double, height: Double, father: Panel?) : SettingButton<Boolean>() {
    init {
        this.width = width
        this.height = height
        this.father = father
        this.value = value
    }
    private var animation = 0f

    companion object {
        private fun deltaTime(): Double {
            return if (FrameRateCounter.fps > 0) 1.0000 / FrameRateCounter.fps else 1.0
        }

        fun fast(end: Float, start: Float, multiple: Float): Float {
            return (1 - MathUtils.clamp(
                (deltaTime() * multiple).toFloat(),
                0f,
                1f
            )) * end + MathUtils.clamp((deltaTime() * multiple).toFloat(), 0f, 1f) * start
        }
    }

    override fun render(context: DrawContext, mouseX: Double, mouseY: Double, partialTicks: Float) {
        animation = fast(
            animation,
            (if (value.value) 1 else 0).toFloat(),
            15f
        )
        val paddingX = (7 * animation).toDouble()
        Render2DEngine.drawRound(
            context.matrices,
            (x + width - 21).toFloat(),
            (y + height / 2 - 4).toFloat(),
            15f,
            8f,
            4f,
            if (paddingX > 4) Colors.getColor(200) else Color(-0x4d4e4f)
        )
        Render2DEngine.drawRound(
            context.matrices,
            (x + width - 20 + paddingX).toFloat(), (y + height / 2 - 3).toFloat(), 6f, 6f, 3f, Color(-1)
        )
        FontRenderers.default.drawString(
            context.matrices, value.name,
            (x + 6.0), (y + height / 2.0 - 6.0 / 2.0).toInt() + 2.0, Color(-1).rgb
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!value.isVisible() || !isHovered(mouseX, mouseY)) {
            return false
        }
        if (button == 0) {
            value.value = !value.value
        }
        return true
    }

    override fun close() {
        animation = 0f
    }
}
