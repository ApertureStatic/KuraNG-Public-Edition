package base.system.render.newfont

import net.minecraft.client.util.math.MatrixStack
import java.awt.Font

class RendererFontAdapter(fnt: Font, var size: Float) : FontAdapter {
    private val fontRenderer = FontRenderer(fnt, size.toInt())

    override fun drawString(matrices: MatrixStack, text: String, x: Float, y: Float, color: Int) {
        var color1 = color
        if (color1 and -0x4000000 == 0) {
            color1 = color1 or -0x1000000
        }
        val a = (color1 shr 24 and 255).toFloat() / 255.0f
        val r = (color1 shr 16 and 255).toFloat() / 255.0f
        val g = (color1 shr 8 and 255).toFloat() / 255.0f
        val b = (color1 and 255).toFloat() / 255.0f
        drawString(matrices, text, x, y, r, g, b, a)
    }

    override fun drawString(matrices: MatrixStack, text: String, x: Double, y: Double, color: Int) {
        drawString(matrices, text, x.toFloat(), y.toFloat(), color)
    }

    override fun drawString(
        matrices: MatrixStack,
        text: String,
        x: Float,
        y: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val v = (a * 255f).toInt() / 255f
        fontRenderer.drawString(matrices, text, x, y - 3, r, g, b, v)
    }

    override fun drawGradientString(matrices: MatrixStack, s: String, x: Float, y: Float, offset: Int) {
        fontRenderer.drawGradientString(matrices, s, x, y - 3, offset)
    }

    override fun drawCenteredString(matrices: MatrixStack, text: String, x: Double, y: Double, color: Int) {
        var color1 = color
        if (color1 and -0x4000000 == 0) {
            color1 = color1 or -0x1000000
        }
        val a = (color1 shr 24 and 255).toFloat() / 255.0f
        val r = (color1 shr 16 and 255).toFloat() / 255.0f
        val g = (color1 shr 8 and 255).toFloat() / 255.0f
        val b = (color1 and 255).toFloat() / 255.0f
        drawCenteredString(matrices, text, x, y, r, g, b, a)
    }

    override fun drawCenteredString(
        matrices: MatrixStack,
        text: String,
        x: Double,
        y: Double,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        fontRenderer.drawCenteredString(matrices, text, x.toFloat(), y.toFloat(), r, g, b, a)
    }

    override fun getStringWidth(text: String): Float {
        return fontRenderer.getStringWidth(text)
    }

    override val fontHeight: Float
        get() = fontRenderer.fontHeight

    override fun getFontHeight(text: String): Float {
        return fontHeight
    }

    override val marginHeight: Float
        get() = fontHeight

    override fun drawString(matrices: MatrixStack, s: String, x: Float, y: Float, color: Int, dropShadow: Boolean) {
        drawString(matrices, s, x, y, color)
    }

    override fun drawString(matrices: MatrixStack, s: String, x: Double, y: Double, color: Int, dropShadow: Boolean) {
        drawString(matrices, s, x, y, color)
    }

    override fun drawString(
        matrices: MatrixStack,
        s: String,
        x: Float,
        y: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        dropShadow: Boolean
    ) {
        drawString(matrices, s, x, y, r, g, b, a)
    }

    override fun trimStringToWidth(`in`: String, width: Double): String {
        val sb = StringBuilder()
        for (c in `in`.toCharArray()) {
            if (getStringWidth(sb.toString() + c) >= width) {
                return sb.toString()
            }
            sb.append(c)
        }
        return sb.toString()
    }

    override fun trimStringToWidth(`in`: String, width: Double, reverse: Boolean): String {
        return trimStringToWidth(`in`, width)
    }
}
