package dev.dyzjct.kura.gui.clickgui.component

import dev.dyzjct.kura.gui.gui.Panel
import dev.dyzjct.kura.setting.ColorSetting
import dev.dyzjct.kura.utils.animations.MathUtils
import base.system.render.graphic.Render2DEngine
import base.system.render.newfont.FontRenderers
import dev.dyzjct.kura.gui.gui.component.SettingButton
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.util.math.MatrixStack
import java.awt.Color

class ColorPickerUtil(colorSetting: ColorSetting, father: Panel?, width: Double, private val normalHeight: Double) :
    SettingButton<Color>() {

    private var hue = 0f
    private var saturation = 0f
    private var brightness = 0f
    private var alpha = 0

    private var afocused = false
    private var hfocused = false
    private var sbfocused = false
    private var extend = false

    private var spos = 0f
    private var bpos = 0f
    private var hpos = 0f
    private var apos = 0f

    private var prevColor: Color? = null

    private var firstInit: Boolean = false

    init {
        value = colorSetting
        this.father = father
        this.width = width
        height = normalHeight
        prevColor = colorSetting.getColorObject()
        updatePos()
        firstInit = true
    }

    override fun render(context: DrawContext, mouseX: Double, mouseY: Double, partialTicks: Float) {
        render(context.matrices, mouseX, mouseY)
    }

    fun render(matrixStack: MatrixStack, mouseX: Double, mouseY: Double) {
        if (!extend) {
            drawContent(matrixStack)
            height = normalHeight
            return
        }
        height = normalHeight + 70
        Render2DEngine.drawRound(
            matrixStack,
            x.toFloat() + 5,
            y.toFloat(),
            width.toFloat() - 10,
            height.toFloat() - 4,
            4f,
            Color(24, 24, 27)
        )
        drawContent(matrixStack)
        renderPicker(matrixStack, mouseX, mouseY, value.value)
    }

    private fun drawContent(matrixStack: MatrixStack) {
        FontRenderers.default.drawString(
            matrixStack,
            value.name,
            x + 7f,
            y + 5f,
            Color(-1).rgb,
            false
        )
        Render2DEngine.drawRectBlurredShadow(
            matrixStack,
            (x + width - 24).toFloat(),
            (y + 3).toFloat(),
            14f,
            6f,
            10,
            value.value
        )
        Render2DEngine.drawRound(
            matrixStack,
            (x + width - 24).toFloat(),
            (y + 3).toFloat(),
            14f,
            6f,
            1f,
            value.value
        )
        FontRenderers.default.drawString(matrixStack, "R", (x + width - 34f), y + 5f, Color(-1).rgb, false)
    }

    private fun isHovered(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        mouseX: Double,
        mouseY: Double
    ): Boolean {
        return mouseX >= x.coerceAtMost(x + width) && mouseX <= x.coerceAtLeast(x + width) && mouseY >= y.coerceAtMost(y + height) && mouseY <= y.coerceAtLeast(
            y + height
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isHovered(x, y, width, normalHeight, mouseX, mouseY)) {
            this.extend = !this.extend
        }
        val cx = (x + 4)
        val cy = (y + 17)
        val cw = (width - 34)
        val ch = (height - 20)
        if (Render2DEngine.isHovered(mouseX, mouseY, cx + cw + 17, cy, 8.0, ch) && button == 0) afocused =
            true else if (Render2DEngine.isHovered(mouseX, mouseY, cx + cw + 4, cy, 8.0, ch) && button == 0) hfocused =
            true else if (Render2DEngine.isHovered(mouseX, mouseY, cx, cy, cw, ch) && button == 0) sbfocused = true
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, state: Int) {
        hfocused = false
        afocused = false
        sbfocused = false
    }

    override fun close() {
        hfocused = false
        afocused = false
        sbfocused = false
    }

    private fun renderPicker(matrixStack: MatrixStack, mouseX: Double, mouseY: Double, color: Color) {
        val cx = (x + 6)
        val cy = (y + 14)
        val cw = (width - 38)
        val ch = (height - 20)
        if (prevColor !== value.value) {
            updatePos()
            prevColor = value.value
        }
        if (firstInit) {
            spos = (cx + cw - (cw - cw * saturation)).toFloat()
            bpos = (cy + (ch - ch * brightness)).toFloat()
            hpos = (cy + (ch - 3 + (ch - 3) * hue)).toFloat()
            apos = (cy + (ch - 3 - (ch - 3) * (alpha / 255f))).toFloat()
            firstInit = false
        }
        spos = Render2DEngine.scrollAnimate(spos, (cx + cw - (cw - cw * saturation)).toFloat(), .6f)
        bpos = Render2DEngine.scrollAnimate(bpos, (cy + (ch - ch * brightness)).toFloat(), .6f)
        hpos = Render2DEngine.scrollAnimate(hpos, (cy + (ch - 3 + (ch - 3) * hue)).toFloat(), .6f)
        apos = Render2DEngine.scrollAnimate(apos, (cy + (ch - 3 - (ch - 3) * (alpha / 255f))).toFloat(), .6f)
        val colorA = Color.getHSBColor(hue, 0.0f, 1.0f)
        val colorB = Color.getHSBColor(hue, 1.0f, 1.0f)
        val colorC = Color(0, 0, 0, 0)
        val colorD = Color(0, 0, 0)
        Render2DEngine.horizontalGradient(matrixStack, cx + 2.0, cy, cx + cw, cy + ch, colorA.rgb, colorB.rgb)
        Render2DEngine.verticalGradient(matrixStack, cx + 2, cy, cx + cw, cy + ch, colorC.rgb, colorD.rgb)
        var i = 1f
        while (i < ch - 2f) {
            val curHue = (1f / (ch / i)).toFloat()
            Render2DEngine.drawRect(
                matrixStack,
                (cx + cw + 4).toFloat(),
                (cy + i).toFloat(),
                8f,
                1f,
                Color.getHSBColor(curHue, 1f, 1f)
            )
            i += 1f
        }
        Render2DEngine.drawRect(
            matrixStack,
            (cx + cw + 17).toFloat(),
            (cy + 1f).toFloat(),
            8f,
            (ch - 3).toFloat(),
            Color(-0x1)
        )
        Render2DEngine.verticalGradient(
            matrixStack,
            cx + cw + 17,
            cy + 0.8,
            cx + cw + 25,
            cy + ch - 2,
            Color(color.red, color.green, color.blue, 255).rgb,
            Color(0, 0, 0, 0).rgb
        )
        Render2DEngine.drawRect(matrixStack, (cx + cw + 3).toFloat(), hpos + 0.5f, 10f, 1f, Color.WHITE)
        Render2DEngine.drawRect(matrixStack, (cx + cw + 16).toFloat(), apos + 0.5f, 10f, 1f, Color.WHITE)
        Render2DEngine.drawRound(matrixStack, spos, bpos, 3f, 3f, 1.5f, Color(-1))
        var value = Color.getHSBColor(hue, saturation, brightness)
        if (sbfocused) {
            saturation = ((MathUtils.clamp((mouseX - cx).toFloat(), 0f, cw.toFloat()) / cw).toFloat())
            brightness = (((ch - MathUtils.clamp((mouseY - cy).toFloat(), 0f, ch.toFloat())) / ch).toFloat())
            value = Color.getHSBColor(hue, saturation, brightness)
            setColor(Color(value.red, value.green, value.blue, alpha))
        }
        if (hfocused) {
            hue = (-((ch - MathUtils.clamp((mouseY - cy).toFloat(), 0f, ch.toFloat())) / ch)).toFloat()
            value = Color.getHSBColor(hue, saturation, brightness)
            setColor(Color(value.red, value.green, value.blue, alpha))
        }
        if (afocused) {
            alpha = ((ch - MathUtils.clamp((mouseY - cy).toFloat(), 0f, ch.toFloat())) / ch * 255f).toInt()
            setColor(Color(value.red, value.green, value.blue, alpha.coerceAtLeast(0)))
        }
    }

    private fun updatePos() {
        val hsb = Color.RGBtoHSB(value.value.red, value.value.green, value.value.blue, null)
        hue = -1 + hsb[0]
        saturation = hsb[1]
        brightness = hsb[2]
        alpha = value.value.alpha
    }

    private fun setColor(color: Color) {
        value.value = color
        prevColor = color
    }

}
