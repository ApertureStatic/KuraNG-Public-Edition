package dev.dyzjct.kura.gui.rewrite.gui

import dev.dyzjct.kura.gui.rewrite.gui.render.DrawDelegate
import dev.dyzjct.kura.module.modules.client.ClickGui
import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import net.minecraft.client.util.math.MatrixStack
import java.awt.Color

object DefaultDrawDelegate : DrawDelegate {
    override val textHeight: Float = FontRenderers.lexend.fontHeight

    override fun drawRoundRect(
        matrixStack: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: Color
    ) {
        Render2DEngine.drawRound(
            matrixStack, x, y, width, height, radius, color
        )
    }

    override fun drawOutlineRect(
        matrixStack: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        lineWidth: Float,
        color: Color
    ) {
        Render2DEngine.renderRectLine(
            matrixStack,
            x,
            y,
            width + 0.5f,
            height,
            lineWidth,
            color.rgb,
            color.rgb,
            color.rgb,
            color.rgb
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
        Render2DEngine.renderRectRoundLine(
            matrixStack,
            x,
            y,
            width + 0.5f,
            height,
            lineWidth,
            radius,
            sample,
            color.rgb,
            color.rgb,
            color.rgb,
            color.rgb
        )
    }

    override fun drawReact(matrixStack: MatrixStack, x: Float, y: Float, width: Float, height: Float, color: Color) {
        Render2DEngine.drawRect(
            matrixStack, x, y, width, height, color
        )
    }

    override fun drawText(matrixStack: MatrixStack, text: String, x: Float, y: Float, color: Color) {
        if (ClickGui.chinese.value) {
            FontRenderers.cn.drawString(
                matrixStack, text, x, y + 2, color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
            )

        } else {
            FontRenderers.lexend.drawString(
                matrixStack, text, x, y + 3, color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
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
