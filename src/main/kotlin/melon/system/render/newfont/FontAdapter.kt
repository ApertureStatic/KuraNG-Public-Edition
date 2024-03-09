package melon.system.render.newfont

import net.minecraft.client.util.math.MatrixStack

interface FontAdapter {
    fun drawString(matrices: MatrixStack, text: String, x: Float, y: Float, color: Int)
    fun drawString(matrices: MatrixStack, text: String, x: Double, y: Double, color: Int)
    fun drawString(matrices: MatrixStack, text: String, x: Float, y: Float, r: Float, g: Float, b: Float, a: Float)
    fun drawGradientString(matrices: MatrixStack, s: String, x: Float, y: Float, offset: Int)
    fun drawCenteredString(matrices: MatrixStack, text: String, x: Double, y: Double, color: Int)
    fun drawCenteredString(
        matrices: MatrixStack,
        text: String,
        x: Double,
        y: Double,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    )

    fun getStringWidth(text: String): Float
    val fontHeight: Float
    fun getFontHeight(text: String): Float
    val marginHeight: Float
    fun drawString(matrices: MatrixStack, s: String, x: Float, y: Float, color: Int, dropShadow: Boolean)
    fun drawString(matrices: MatrixStack, s: String, x: Double, y: Double, color: Int, dropShadow: Boolean)
    fun drawString(
        matrices: MatrixStack,
        s: String,
        x: Float,
        y: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        dropShadow: Boolean
    )

    fun trimStringToWidth(text: String, width: Double): String
    fun trimStringToWidth(text: String, width: Double, reverse: Boolean): String
}
