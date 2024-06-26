package dev.dyzjct.kura.gui.clickgui.render

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.util.math.MatrixStack
import java.awt.Color

class DrawScope(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val drawDelegate: DrawDelegate,
    val context: DrawContext,
    val matrixStack: MatrixStack = context.matrices
) {
    val textHeight = drawDelegate.textHeight

    fun getTextWidth(text: String): Float {
        return drawDelegate.getStringWidth(text)
    }

    fun drawRect(
        x: Float, y: Float, width: Float, height: Float, color: Color, padding: Padding = Padding.Empty
    ) {
        drawDelegate.drawReact(
            matrixStack,
            x + padding.left,
            y + padding.top,
            (width - padding.left - padding.right).coerceAtLeast(0f),
            (height - padding.top - padding.bottom).coerceAtLeast(0f),
            color
        )
    }

    fun drawRoundRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: Color,
        padding: Padding = Padding.Empty
    ) {
        drawDelegate.drawRoundRect(
            matrixStack,
            x + padding.left,
            y + padding.top,
            (width - padding.left - padding.right).coerceAtLeast(0f),
            (height - padding.top - padding.bottom).coerceAtLeast(0f),
            radius,
            color
        )
    }

    fun drawRoundRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        padding: Padding = Padding.Empty
    ) {
        drawDelegate.drawRoundRect(
            matrixStack,
            x + padding.left,
            y + padding.top,
            (width - padding.left - padding.right).coerceAtLeast(0f),
            (height - padding.top - padding.bottom).coerceAtLeast(0f),
            4f,
            color
        )
    }


    fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: Color,
        padding: Padding = Padding.Empty,
        verticalAlignment: Alignment = Alignment.START,
        horizontalAlignment: Alignment = Alignment.START,
        containerHeight: Float = height,
        containerWidth: Float = width
    ) {
        val offsetX = when (horizontalAlignment) {
            Alignment.START -> padding.left
            Alignment.CENTER -> ((containerWidth - drawDelegate.getStringWidth(text)) / 2) + padding.left
            Alignment.END -> containerWidth - drawDelegate.getStringWidth(text) - padding.left
        }

        val offsetY = when (verticalAlignment) {
            Alignment.START -> padding.top
            Alignment.CENTER -> ((containerHeight - textHeight) / 2) + padding.top
            Alignment.END -> containerHeight - textHeight - padding.top
        }

        drawDelegate.drawText(matrixStack, text, x + offsetX, y + offsetY, color)
    }

    fun drawText(
        text: String,
        color: Color,
        padding: Padding = Padding.Empty,
        verticalAlignment: Alignment = Alignment.START,
        horizontalAlignment: Alignment = Alignment.START,
        containerHeight: Float = height,
        containerWidth: Float = width
    ) {
        drawText(text, x, y, color, padding, verticalAlignment, horizontalAlignment, containerHeight, containerWidth)
    }

    fun drawOutlineRectBySetting(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        padding: Padding = Padding.Empty
    ) {
        drawOutlineRect(x, y, width, height, color, padding)
    }

    fun drawOutlineRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        padding: Padding = Padding.Empty
    ) {
        drawDelegate.drawOutlineRect(
            matrixStack,
            x + padding.left,
            y + padding.top,
            (width - padding.left - padding.right).coerceAtLeast(0f),
            (height - padding.top - padding.bottom).coerceAtLeast(0f),
            1f,
            color
        )
    }

    fun drawOutlineRoundRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float = 1f,
        padding: Padding = Padding.Empty
    ) {
        drawDelegate.drawOutlineRoundRect(
            matrixStack,
            x + padding.left,
            y + padding.top,
            (width - padding.left - padding.right).coerceAtLeast(0f),
            (height - padding.top - padding.bottom).coerceAtLeast(0f),
            2f,
            360f,
            lineWidth,
            color
        )
    }
}