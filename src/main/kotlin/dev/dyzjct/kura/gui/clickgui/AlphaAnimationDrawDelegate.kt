package dev.dyzjct.kura.gui.clickgui

import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import dev.dyzjct.kura.gui.clickgui.render.DrawDelegate
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.utils.animations.Easing
import net.minecraft.client.util.math.MatrixStack
import java.awt.Color

class AlphaAnimationDrawDelegate : DrawDelegate {
    private var startTime = System.currentTimeMillis()
    private val alphaPercent
        get() = Easing.IN_QUAD.inc(Easing.toDelta(startTime, UiSetting.animationLength))

    var isReverse = false

    val isAnimationFinished
        get() = startTime + UiSetting.animationLength < System.currentTimeMillis()

    override val textHeight: Float = FontRenderers.lexend.fontHeight

    fun resetAnimation() {
        startTime = System.currentTimeMillis()
    }

    private fun calculateColorAlpha(color: Color): Color {
        return Color(
            color.red,
            color.green,
            color.blue,
            (color.alpha * if (isReverse) (1f - alphaPercent) else alphaPercent).toInt()
        )
    }

    override fun drawRoundRect(
        matrixStack: MatrixStack, x: Float, y: Float, width: Float, height: Float, radius: Float, color: Color
    ) {
        Render2DEngine.drawRound(
            matrixStack, x, y, width, height, radius, calculateColorAlpha(color)
        )
    }

    override fun drawOutlineRect(
        matrixStack: MatrixStack, x: Float, y: Float, width: Float, height: Float, lineWidth: Float, color: Color
    ) {
        val calculatedColor = calculateColorAlpha(color)
        Render2DEngine.renderRectLine(
            matrixStack,
            x,
            y,
            width + 0.5f,
            height,
            lineWidth,
            calculatedColor.rgb,
            calculatedColor.rgb,
            calculatedColor.rgb,
            calculatedColor.rgb
        )
    }

    override fun drawOutlineRoundRect(
        matrixStack: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        sample: Float,
        lineWidth: Float,
        color: Color
    ) {
        val calculatedColor = calculateColorAlpha(color)
        Render2DEngine.renderRectRoundLine(
            matrixStack,
            x,
            y,
            width + 0.5f,
            height,
            lineWidth,
            radius,
            sample,
            calculatedColor.rgb,
            calculatedColor.rgb,
            calculatedColor.rgb,
            calculatedColor.rgb
        )
    }

    override fun drawReact(matrixStack: MatrixStack, x: Float, y: Float, width: Float, height: Float, color: Color) {
        Render2DEngine.drawRect(
            matrixStack, x, y, width, height, calculateColorAlpha(color)
        )
    }

    override fun drawText(matrixStack: MatrixStack, text: String, x: Float, y: Float, color: Color) {
        val calculatedColor = calculateColorAlpha(color)
        if (ClickGui.chinese.value) {
            FontRenderers.cn.drawString(
                matrixStack,
                text,
                x,
                y + 2,
                calculatedColor.red / 255f,
                calculatedColor.green / 255f,
                calculatedColor.blue / 255f,
                calculatedColor.alpha / 255f
            )

        } else {
            FontRenderers.lexend.drawString(
                matrixStack,
                text,
                x,
                y + 3,
                calculatedColor.red / 255f,
                calculatedColor.green / 255f,
                calculatedColor.blue / 255f,
                calculatedColor.alpha / 255f
            )
        }
    }

    override fun getStringWidth(text: String): Float {
        return if (ClickGui.chinese.value) {
            FontRenderers.cn
        } else {
            FontRenderers.lexend
        }.getStringWidth(text)
    }
}