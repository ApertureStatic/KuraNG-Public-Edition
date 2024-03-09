package dev.dyzjct.kura.gui.rewrite.gui.render

import dev.dyzjct.kura.gui.rewrite.gui.DefaultDrawDelegate
import net.minecraft.client.util.math.MatrixStack
import java.awt.Color

interface DrawDelegate {
    fun drawReact(
        matrixStack: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color
    )

    fun drawRoundRect(
        matrixStack: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: Color
    )

    fun drawOutlineRect(
        matrixStack: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        lineWidth: Float,
        color: Color
    )

    fun drawOutlineRoundRect(
        matrixStack: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        sample: Float,
        lineWidth: Float,
        color: Color
    )

    fun drawText(
        matrixStack: MatrixStack,
        text: String,
        x: Float,
        y: Float,
        color: Color
    )

    val textHeight: Float

    fun getStringWidth(text: String): Float

    companion object {
        @JvmStatic
        val defaultDrawDelegate = DefaultDrawDelegate
    }
}