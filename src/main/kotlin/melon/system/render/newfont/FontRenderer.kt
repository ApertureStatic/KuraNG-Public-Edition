package melon.system.render.newfont

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.modules.client.Colors
import dev.dyzjct.kura.utils.animations.MathUtils.roundToDecimal
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Util
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import java.awt.Font
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class FontRenderer(val f: Font, val size: Int) {
    //TODO Find A Better Font To Replace Current Candy-Mono Font
    private val fallbackFont = Font.createFont(
        Font.TRUETYPE_FONT, Objects.requireNonNull(
            Kura::class.java.classLoader.getResourceAsStream("chinese.ttf")
        )
    ).deriveFont(Font.PLAIN, size.toFloat())
    private val glyphMap: MutableMap<Char, Glyph> = ConcurrentHashMap()
    val fontHeight: Float

    init {
        init()
        fontHeight = glyphMap.values
            .stream()
            .max(Comparator.comparingDouble { it.dimensions.height })
            .orElseThrow().dimensions.height.toFloat() * 0.25f
    }

    fun init() {
        val chars = "ABCabc 123+-".toCharArray()
        for (aChar in chars) {
            val glyph = Glyph(aChar, f, fallbackFont)
            glyphMap[aChar] = glyph
        }
    }

    fun drawString(matrices: MatrixStack, s: String, x: Float, y: Float, r: Float, g: Float, b: Float, a: Float) {
        val roundedX = roundToDecimal(x.toDouble(), 1).toFloat()
        val roundedY = roundToDecimal(y.toDouble(), 1).toFloat()
        var r1 = r
        var g1 = g
        var b1 = b
        matrices.push()
        matrices.translate(roundedX, roundedY, 0f)
        matrices.scale(0.25f, 0.25f, 1f)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        // RenderSystem.enableTexture();
        RenderSystem.disableCull()
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        RenderSystem.setShader { GameRenderer.getPositionTexColorProgram() }
        val bufferBuilder = Tessellator.getInstance().buffer
        var isInSelector = false
        for (c in s.toCharArray()) {
            if (isInSelector) {
                val upper = c.toString().uppercase(Locale.getDefault())[0]
                val color = colorMap[upper] ?: 0xFFFFFF
                r1 = (color shr 16 and 255).toFloat() / 255.0f
                g1 = (color shr 8 and 255).toFloat() / 255.0f
                b1 = (color and 255).toFloat() / 255.0f
                isInSelector = false
                continue
            }
            if (c == '§') {
                isInSelector = true
                continue
            }
            val matrix = matrices.peek().positionMatrix ?: return
            val prevWidth = drawChar(bufferBuilder, matrix, c, r1, g1, b1, a)
            matrices.translate(prevWidth, 0.0, 0.0)
        }
        matrices.pop()
    }

    fun drawGradientString(matrices: MatrixStack, s: String, x: Float, y: Float, offset: Int) {
        val roundedX = roundToDecimal(x.toDouble(), 1).toFloat()
        val roundedY = roundToDecimal(y.toDouble(), 1).toFloat()
        var r1: Float
        var g1: Float
        var b1: Float
        matrices.push()
        matrices.translate(roundedX, roundedY, 0f)
        matrices.scale(0.25f, 0.25f, 1f)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        // RenderSystem.enableTexture();
        RenderSystem.disableCull()
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        RenderSystem.setShader { GameRenderer.getPositionTexColorProgram() }
        val bufferBuilder = Tessellator.getInstance().buffer
        for ((num, c) in s.toCharArray().withIndex()) {
            val color = Colors.getColor(num * offset).rgb
            r1 = (color shr 16 and 255).toFloat() / 255.0f
            g1 = (color shr 8 and 255).toFloat() / 255.0f
            b1 = (color and 255).toFloat() / 255.0f
            val matrix = matrices.peek().positionMatrix ?: return
            val prevWidth = drawChar(bufferBuilder, matrix, c, r1, g1, b1, 1f)
            matrices.translate(prevWidth, 0.0, 0.0)
        }
        matrices.pop()
    }

    private fun stripControlCodes(`in`: String): String {
        val s = `in`.toCharArray()
        val out = StringBuilder()
        var i = 0
        while (i < s.size) {
            val current = s[i]
            if (current == '§') {
                i++
                i++
                continue
            }
            out.append(current)
            i++
        }
        return out.toString()
    }

    fun getStringWidth(text: String): Float {
        var wid = 0f
        for (c in stripControlCodes(text).toCharArray()) {
            val g = glyphMap.computeIfAbsent(c) { Glyph(it, f, fallbackFont) }
            wid += g.dimensions.width.toFloat()
        }
        return wid * 0.25f
    }

    fun trimStringToWidth(t: String, maxWidth: Float): String {
        val sb = StringBuilder()
        for (c in t.toCharArray()) {
            if (getStringWidth(sb.toString() + c) >= maxWidth) {
                return sb.toString()
            }
            sb.append(c)
        }
        return sb.toString()
    }

    fun drawCenteredString(
        matrices: MatrixStack,
        s: String,
        x: Float,
        y: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        drawString(matrices, s, x - getStringWidth(s) / 2f, y, r, g, b, a)
    }

    private fun drawChar(
        bufferBuilder: BufferBuilder,
        matrix: Matrix4f,
        c: Char,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ): Double {
        val glyph = glyphMap.computeIfAbsent(c) { character: Char -> Glyph(character, f, fallbackFont) }
        RenderSystem.setShaderTexture(0, glyph.imageTex)
        val height = glyph.dimensions.height.toFloat()
        val width = glyph.dimensions.width.toFloat()
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        bufferBuilder.vertex(matrix, 0f, height, 0f).texture(0f, 1f).color(r, g, b, a).next()
        bufferBuilder.vertex(matrix, width, height, 0f).texture(1f, 1f).color(r, g, b, a).next()
        bufferBuilder.vertex(matrix, width, 0f, 0f).texture(1f, 0f).color(r, g, b, a).next()
        bufferBuilder.vertex(matrix, 0f, 0f, 0f).texture(0f, 0f).color(r, g, b, a).next()
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        return width.toDouble()
    }

    companion object {
        private val colorMap = Util.make<Map<Char, Int>> {
            val ci: MutableMap<Char, Int> = HashMap()
            ci['0'] = 0x000000
            ci['1'] = 0x0000AA
            ci['2'] = 0x00AA00
            ci['3'] = 0x00AAAA
            ci['4'] = 0xAA0000
            ci['5'] = 0xAA00AA
            ci['6'] = 0xFFAA00
            ci['7'] = 0xAAAAAA
            ci['8'] = 0x555555
            ci['9'] = 0x5555FF
            ci['A'] = 0x55FF55
            ci['B'] = 0x55FFFF
            ci['C'] = 0xFF5555
            ci['D'] = 0xFF55FF
            ci['E'] = 0xFFFF55
            ci['F'] = 0xFFFFFF
            ci
        }
    }
}
