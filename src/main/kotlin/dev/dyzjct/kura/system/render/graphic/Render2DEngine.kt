package dev.dyzjct.kura.system.render.graphic

import base.utils.math.vector.Vec2f
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.system.util.interfaces.MinecraftWrapper
import dev.dyzjct.kura.utils.animations.MathUtils
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.*
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import org.apache.commons.lang3.RandomStringUtils
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.*

object Render2DEngine : MinecraftWrapper {
    fun addWindow(stack: MatrixStack, r1: Rectangle) {
        val matrix = stack.peek().positionMatrix
        val coord = Vector4f(r1.x, r1.y, 0f, 1f).mulTranspose(matrix)
        val end = Vector4f(r1.x1, r1.y1, 0f, 1f).mulTranspose(matrix)
        val r = Rectangle(coord.x, coord.y, end.x, end.y)
        if (clipStack.empty()) {
            clipStack.push(r)
            beginScissor(r.x, r.y, r.x1, r.y1)
        } else {
            val lastClip = clipStack.peek()
            val lsx = lastClip.x
            val lsy = lastClip.y
            val lstx = lastClip.x1
            val lsty = lastClip.y1
            val nsx = MathHelper.clamp(r.x, lsx, lstx)
            val nsy = MathHelper.clamp(r.y, lsy, lsty)
            val nstx = MathHelper.clamp(r.x1, nsx, lstx)
            val nsty = MathHelper.clamp(r.y1, nsy, lsty)
            clipStack.push(Rectangle(nsx, nsy, nstx, nsty))
            beginScissor(nsx, nsy, nstx, nsty)
        }
    }

    val clipStack = Stack<Rectangle>()

    @JvmStatic
    fun popWindow() {
        clipStack.pop()
        if (clipStack.empty()) {
            endScissor()
        } else {
            val r = clipStack.peek()
            beginScissor(r.x, r.y, r.x1, r.y1)
        }
    }

    fun beginScissor(x: Float, y: Float, endX: Float, endY: Float) {
        val width = max(0f, endX - x)
        val height = max(0f, endY - y)
        val mcScaleFactor = mc.window.scaleFactor.toFloat()
        val ay = ((mc.window.scaledHeight - (y + height)) * mcScaleFactor).toInt()
        RenderSystem.enableScissor(
            (x * mcScaleFactor).toInt(),
            ay,
            (width * mcScaleFactor).toInt(),
            (height * mcScaleFactor).toInt()
        )
    }

    fun endScissor() {
        RenderSystem.disableScissor()
    }

    @JvmStatic
    fun addWindow(stack: MatrixStack, x: Float, y: Float, x1: Float, y1: Float, animationFactor: Double) {
        val h = y + y1
        val h2 = (h * (1.0 - MathUtils.clamp(animationFactor, 0.0, 1.0025))).toFloat()
        val y3 = y + h2
        var x4 = x1
        var y4 = y1 - h2
        if (x4 < x) {
            x4 = x
        }
        if (y4 < y3) {
            y4 = y3
        }
        addWindow(stack, Rectangle(x, y3, x4, y4))
    }

    fun drawTexture(context: DrawContext, icon: Identifier, x: Int, y: Int, width: Int, height: Int) {
        RenderSystem.blendEquation(GL14.GL_FUNC_ADD)
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
        RenderSystem.enableBlend()
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR)
        context.drawTexture(icon, x, y, 0f, 0f, width, height, width, height)
    }

    fun drawQuad(matrices: MatrixStack, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        renderQuad(matrices, left, top, right, bottom, color, color, color, color)
    }

    fun drawQuad(matrices: MatrixStack, left: Float, top: Float, right: Float, bottom: Float, color: Color) {
        drawQuad(matrices, left, top, right, bottom, color.rgb)
    }

    fun drawRect(matrices: MatrixStack, x: Float, y: Float, width: Float, height: Float, color: Int) {
        drawQuad(matrices, x, y, x + width, y + height, color)
    }

    fun drawRect(matrices: MatrixStack, x: Float, y: Float, width: Float, height: Float, color: Color) {
        drawRect(matrices, x, y, width, height, color.rgb)
    }

    fun drawQuadHorizontalGradient(
        matrices: MatrixStack,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        startColor: Color,
        endColor: Color
    ) {
        drawQuadHorizontalGradient(
            matrices,
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            startColor,
            endColor
        )
    }

    fun drawQuadHorizontalGradient(
        matrices: MatrixStack,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        startColor: Int,
        endColor: Int
    ) {
        drawQuadHorizontalGradient(
            matrices,
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            Color(startColor),
            Color(endColor)
        )
    }

    fun drawQuadHorizontalGradient(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startColor: Color,
        endColor: Color
    ) {
        drawQuadHorizontalGradient(matrices, left, top, right, bottom, startColor.rgb, endColor.rgb)
    }

    fun drawQuadHorizontalGradient(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startColor: Int,
        endColor: Int
    ) {
        renderQuad(matrices, left, top, right, bottom, startColor, startColor, endColor, endColor)
    }

    fun drawQuadVerticalGradient(
        matrices: MatrixStack,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        startColor: Color,
        endColor: Color
    ) {
        drawQuadVerticalGradient(
            matrices,
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            startColor.rgb,
            endColor.rgb
        )
    }

    fun drawQuadVerticalGradient(
        matrices: MatrixStack,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        startColor: Int,
        endColor: Int
    ) {
        drawQuadVerticalGradient(
            matrices,
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            startColor,
            endColor
        )
    }

    fun drawQuadVerticalGradient(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startColor: Color,
        endColor: Color
    ) {
        drawQuadVerticalGradient(matrices, left, top, right, bottom, startColor.rgb, endColor.rgb)
    }

    fun drawQuadVerticalGradient(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startColor: Int,
        endColor: Int
    ) {
        renderQuad(matrices, left, top, right, bottom, startColor, endColor, startColor, endColor)
    }

    fun drawRectBlurredShadow(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blurRadius: Int,
        color: Int
    ) {
        renderRectBlurredShadow(matrices, x, y, width, height, blurRadius, color, color, color, color)
    }

    fun drawRectBlurredShadow(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blurRadius: Int,
        color: Color
    ) {
        drawRectBlurredShadow(matrices, x, y, width, height, blurRadius, color.rgb)
    }

    fun drawRectGradientBlurredShadow(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blurRadius: Int,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        renderRectBlurredShadow(
            matrices,
            x,
            y,
            width,
            height,
            blurRadius,
            leftTopColor,
            leftBottomColor,
            rightTopColor,
            rightBottomColor
        )
    }

    fun drawRectGradientBlurredShadow(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blurRadius: Int,
        leftTopColor: Color,
        leftBottomColor: Color,
        rightTopColor: Color,
        rightBottomColor: Color
    ) {
        renderRectBlurredShadow(
            matrices,
            x,
            y,
            width,
            height,
            blurRadius,
            leftTopColor.rgb,
            leftBottomColor.rgb,
            rightTopColor.rgb,
            rightBottomColor.rgb
        )
    }

    fun horizontalGradient(
        matrices: MatrixStack,
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        startColor: Int,
        endColor: Int
    ) {
        drawQuadHorizontalGradient(matrices, x1, y1, x2, y2, startColor, endColor)
    }

    fun verticalGradient(
        matrices: MatrixStack,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        startColor: Int,
        endColor: Int
    ) {
        drawQuadVerticalGradient(matrices, left, top, right, bottom, startColor, endColor)
    }

    fun drawRectDumbWay(
        matrices: MatrixStack, x: Float, y: Float, x1: Float, y1: Float, c1: Color, c2: Color, c3: Color, c4: Color
    ) {
        val matrix = matrices.peek().positionMatrix
        val bufferBuilder = Tessellator.getInstance().buffer
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        bufferBuilder.vertex(matrix, x, y1, 0.0f).color(c1.rgb).next()
        bufferBuilder.vertex(matrix, x1, y1, 0.0f).color(c2.rgb).next()
        bufferBuilder.vertex(matrix, x1, y, 0.0f).color(c3.rgb).next()
        bufferBuilder.vertex(matrix, x, y, 0.0f).color(c4.rgb).next()
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
    }


    fun registerBufferedImageTexture(identifier: Identifier, image: BufferedImage) {
        try {
            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "png", baos)
            val bytes = baos.toByteArray()
            registerTexture(identifier, bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun registerTexture(identifier: Identifier, content: ByteArray) {
        try {
            val data = BufferUtils.createByteBuffer(content.size).put(content).also { it.flip() }
            val tex = NativeImageBackedTexture(NativeImage.read(data))
            mc.execute { mc.textureManager.registerTexture(identifier, tex) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun renderTexture(
        matrices: MatrixStack,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        u: Float,
        v: Float,
        regionWidth: Double,
        regionHeight: Double,
        textureWidth: Double,
        textureHeight: Double
    ) {
        renderTextureRect(
            matrices,
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            u,
            v,
            regionWidth.toFloat(),
            regionHeight.toFloat(),
            textureWidth.toFloat(),
            textureHeight.toFloat(),
        )
    }

    fun drawElipse(x: Float, y: Float, rx: Float, ry: Float, start: Float, end: Float, radius: Float, color: Color) {
        var start = start
        var end = end
        if (start > end) {
            val endOffset = end
            end = start
            start = endOffset
        }
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)
        RenderSystem.lineWidth(100f)
        setupRender()
        var i = start
        while (i <= end) {
            val cos = cos(i * Math.PI / 180).toFloat() * (radius / ry)
            val sin = sin(i * Math.PI / 180).toFloat() * (radius / rx)
            bufferBuilder.vertex((x + cos).toDouble(), (y + sin).toDouble(), 0.0)
                .color(color.red, color.green, color.blue, color.alpha).next()
            i += 4f
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        RenderSystem.lineWidth(1f)
        RenderSystem.disableBlend()
        endRender()
    }

    fun drawElipseSync(
        x: Float, y: Float, rx: Float, ry: Float, start0: Float, end0: Float, radius: Float, color: Color
    ) {
        var start = start0
        var end = end0
        if (start > end) {
            val endOffset = end
            end = start
            start = endOffset
        }
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)
        RenderSystem.lineWidth(100f)
        setupRender()
        var i = start
        while (i <= end) {
            val cos = cos(i * Math.PI / 180).toFloat() * (radius / ry)
            val sin = sin(i * Math.PI / 180).toFloat() * (radius / rx)
            bufferBuilder.vertex((x + cos).toDouble(), (y + sin).toDouble(), 0.0).color(color.rgb).next()
            i += 4f
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        RenderSystem.lineWidth(1f)
        RenderSystem.disableBlend()
        endRender()
    }

    fun drawRound(matrices: MatrixStack, x: Float, y: Float, width: Float, height: Float, radius: Float, color: Color) {
        renderRoundedQuad(
            matrices,
            color,
            x.toDouble(),
            y.toDouble(),
            (width + x).toDouble(),
            (height + y).toDouble(),
            radius.toDouble(),
            4.0
        )
    }

    fun drawRoundD(
        matrices: MatrixStack,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        radius: Float,
        color: Color
    ) {
        renderRoundedQuad(matrices, color, x, y, width + x, height + y, radius.toDouble(), 4.0)
    }

    fun drawRoundDoubleColor(
        matrices: MatrixStack,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        radius: Float,
        color: Color,
        color2: Color
    ) {
        renderRoundedQuad(matrices, color, color2, x, y, width + x, height + y, radius.toDouble(), 4.0)
    }

    fun renderRoundedQuad(
        matrices: MatrixStack,
        c: Color,
        fromX: Double,
        fromY: Double,
        toX: Double,
        toY: Double,
        radius: Double,
        samples: Double
    ) {
        renderQuadRound(
            matrices,
            fromX.toFloat(),
            fromY.toFloat(),
            toX.toFloat(),
            toY.toFloat(),
            radius.toInt(),
            samples.toInt(),
            c.rgb,
            c.rgb,
            c.rgb,
            c.rgb
        )
    }

    fun renderRoundedQuad(
        matrices: MatrixStack,
        c: Color,
        c2: Color,
        fromX: Double,
        fromY: Double,
        toX: Double,
        toY: Double,
        radius: Double,
        samples: Double
    ) {
        renderQuadRound(
            matrices,
            fromX.toFloat(),
            fromY.toFloat(),
            toX.toFloat(),
            toY.toFloat(),
            radius.toInt(),
            samples.toInt(),
            c.rgb,
            c.rgb,
            c2.rgb,
            c2.rgb
        )
    }

    // -------------------- Render --------------------
    fun renderQuad(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        val matrix = matrices.peek().positionMatrix
        val bufferBuilder = Tessellator.getInstance().buffer
        setupRender()
        RenderSystem.setShader(GameRenderer::getPositionColorProgram)
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        bufferBuilder.vertex(matrix, left, top, 0.0f).color(leftTopColor).next()
        bufferBuilder.vertex(matrix, left, bottom, 0.0f).color(leftBottomColor).next()
        bufferBuilder.vertex(matrix, right, bottom, 0.0f).color(rightBottomColor).next()
        bufferBuilder.vertex(matrix, right, top, 0.0f).color(rightTopColor).next()
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
    }

    fun renderRect(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        renderQuad(
            matrices,
            x,
            y,
            x + width,
            y + height,
            leftTopColor,
            leftBottomColor,
            rightTopColor,
            rightBottomColor
        )
    }

    fun renderQuadRound(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Int,
        samples: Int,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        val radius = min(min(radius, abs(left - right).toInt()), abs(top - bottom).toInt())
        val matrix = matrices.peek().positionMatrix
        val bufferBuilder = Tessellator.getInstance().buffer
        setupRender()
        RenderSystem.setShader(GameRenderer::getPositionColorProgram)
        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR)
        val map = arrayOf(
            floatArrayOf(right - radius, bottom - radius),
            floatArrayOf(right - radius, top + radius),
            floatArrayOf(left + radius, top + radius),
            floatArrayOf(left + radius, bottom - radius)
        )
        val colorMap = intArrayOf(rightBottomColor, rightTopColor, leftTopColor, leftBottomColor)
        for (i in 0..3) {
            val current = map[i]
            val currentColor = colorMap[i]
            var r = i * 90.0
            while (r < 90 + i * 90) {
                val angle = Math.toRadians(r).toFloat()
                val sin = sin(angle) * radius
                val cos = cos(angle) * radius
                if (i <= 1) bufferBuilder.vertex(matrix, current[0] + sin, current[1] + cos, 0f)
                    .color(currentColor).next() else
                    bufferBuilder.vertex(matrix, current[0] + sin, current[1] + cos, 0f).color(currentColor).next()
                r += 90 / samples
            }
//            val angle = Math.toRadians((i + 1) * 90.0).toFloat()
//            val sin = sin(angle) * radius
//            val cos = cos(angle) * radius
//            bufferBuilder.vertex(matrix, current[0] + sin, current[1] + cos, 0f).color(currentColor).next()
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
    }

    fun renderRectRound(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Int,
        samples: Int,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        renderQuadRound(
            matrices,
            x,
            y,
            x + width,
            y + height,
            radius,
            samples,
            leftTopColor,
            leftBottomColor,
            rightTopColor,
            rightBottomColor
        )
    }

    private val shadowCache = HashMap<Int, BlurredShadow>()

    fun renderQuadBlurredShadow(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        blurRadius: Int,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        val width = abs(right - left) + blurRadius * 2f
        val height = abs(bottom - top) + blurRadius * 2f
        val identifier = (width * height + width * blurRadius).toInt()
        if (shadowCache.containsKey(identifier)) {
            shadowCache[identifier]?.bind()
        } else {
            val bufferedImage = BufferedImage(abs(width.toInt()), abs(height.toInt()), BufferedImage.TYPE_INT_ARGB)
            bufferedImage.graphics.let {
                it.color = Color(-1)
                it.fillRect(blurRadius, blurRadius, (width - blurRadius * 2).toInt(), (height - blurRadius * 2).toInt())
                it.dispose()
            }
            val blurred = GaussianFilter(blurRadius).filter(bufferedImage, null)
            shadowCache[identifier] = BlurredShadow(blurred)
            return
        }
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableBlend()
        renderTextureQuad(
            matrices,
            left - blurRadius,
            top - blurRadius,
            right + blurRadius,
            bottom + blurRadius,
            0f,
            0f,
            width,
            height,
            width,
            height,
            leftTopColor,
            leftBottomColor,
            rightTopColor,
            rightBottomColor
        )
        RenderSystem.disableBlend()
    }

    fun renderRectBlurredShadow(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blurRadius: Int,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        renderQuadBlurredShadow(
            matrices,
            x,
            y,
            x + width,
            y + height,
            blurRadius,
            leftTopColor,
            leftBottomColor,
            rightTopColor,
            rightBottomColor
        )
    }

    fun renderLine(
        matrices: MatrixStack,
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        lineWidth: Float,
        fromColor: Int,
        toColor: Int
    ) {
        val matrix = matrices.peek().positionMatrix
        val bufferBuilder = Tessellator.getInstance().buffer
        setupRender()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader(GameRenderer::getPositionColorProgram)
        RenderSystem.lineWidth(lineWidth)
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)
        bufferBuilder.vertex(matrix, fromX, fromY, 0f).color(fromColor).next()
        bufferBuilder.vertex(matrix, toX, toY, 0f).color(toColor).next()
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
    }

    fun renderLineStrip(matrices: MatrixStack, positionsAndColors: Map<Vec2f, Int>, lineWidth: Float) {
        val matrix = matrices.peek().positionMatrix
        val bufferBuilder = Tessellator.getInstance().buffer
        setupRender()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader(GameRenderer::getPositionColorProgram)
        RenderSystem.lineWidth(lineWidth)
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)
        for ((pos, color) in positionsAndColors) {
            bufferBuilder.vertex(matrix, pos.x, pos.y, 0f).color(color).next()
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
    }

    fun renderQuadLine(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        lineWidth: Float,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        val arrayQuad = mapOf(
            Vec2f(left, top) to leftTopColor,
            Vec2f(left, bottom) to leftBottomColor,
            Vec2f(right, bottom) to rightBottomColor,
            Vec2f(right, top) to rightTopColor,
        )
        renderLineStrip(matrices, arrayQuad, lineWidth)
        renderLine(matrices, left, top, right, top, lineWidth, leftTopColor, rightTopColor)
    }

    fun renderRectLine(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        hight: Float,
        lineWidth: Float,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        renderQuadLine(
            matrices,
            x,
            y,
            x + width,
            y + hight,
            lineWidth,
            leftTopColor,
            leftBottomColor,
            rightTopColor,
            rightBottomColor
        )
    }

    fun renderQuadRoundLine(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        lineWidth: Float,
        radius: Float,
        samples: Float,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        val radius = min(min(radius, abs(left - right)), abs(top - bottom))
        val renderMap = mutableMapOf<Vec2f, Int>()
        val map = arrayOf(
            floatArrayOf(right - radius, bottom - radius),
            floatArrayOf(right - radius, top + radius),
            floatArrayOf(left + radius, top + radius),
            floatArrayOf(left + radius, bottom - radius)
        )
        val colorMap = intArrayOf(rightBottomColor, rightTopColor, leftTopColor, leftBottomColor)
        for (i in 0..3) {
            val current = map[i]
            val currentColor = colorMap[i]

            var r = i * 90f
            while (r < i * 90 + 90) {
                val angle = Math.toRadians(r.toDouble()).toFloat()
                val sin = sin(angle) * radius
                val cos = cos(angle) * radius
                renderMap[Vec2f(current[0] + sin, current[1] + cos)] = currentColor
                r += 90 / samples
            }

            val angle = Math.toRadians((i + 1) * 90.0).toFloat()
            val sin = sin(angle) * radius
            val cos = cos(angle) * radius
            renderMap[Vec2f(current[0] + sin, current[1] + cos)] = currentColor
        }
        renderLineStrip(matrices, renderMap, lineWidth)
        renderLine(
            matrices,
            left + radius,
            bottom,
            right - radius,
            bottom,
            lineWidth,
            leftBottomColor,
            rightBottomColor
        )
    }

    fun renderRectRoundLine(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        lineWidth: Float,
        radius: Float,
        samples: Float,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        renderQuadRoundLine(
            matrices,
            x,
            y,
            x + width,
            y + height,
            lineWidth,
            radius,
            samples,
            leftTopColor,
            leftBottomColor,
            rightTopColor,
            rightBottomColor
        )
    }

    fun renderTextureQuad(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        u: Float,
        v: Float,
        regionWidth: Float,
        regionHeight: Float,
        textureWidth: Float,
        textureHeight: Float,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        val matrix = matrices.peek().positionMatrix
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionTexProgram() }
        val bufferBuilder = Tessellator.getInstance().buffer
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
        setShaderColor(leftTopColor)
        bufferBuilder.vertex(matrix, left, top, 0f).texture(u / textureWidth, v / textureHeight).next()
        setShaderColor(leftBottomColor)
        bufferBuilder.vertex(matrix, left, bottom, 0f).texture(u / textureWidth, (v + regionHeight) / textureHeight)
            .next()
        setShaderColor(rightBottomColor)
        bufferBuilder.vertex(matrix, right, bottom, 0f)
            .texture((u + regionWidth) / textureWidth, (v + regionHeight) / textureHeight).next()
        setShaderColor(rightTopColor)
        bufferBuilder.vertex(matrix, right, top, 0f).texture((u + regionWidth) / textureWidth, v / textureHeight).next()
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
    }

    fun renderTextureQuad(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        u: Float,
        v: Float,
        regionWidth: Float,
        regionHeight: Float,
        textureWidth: Float,
        textureHeight: Float
    ) {
        val white = Color.WHITE.rgb
        renderTextureQuad(
            matrices,
            left,
            top,
            right,
            bottom,
            u,
            v,
            regionWidth,
            regionHeight,
            textureWidth,
            textureHeight,
            white,
            white,
            white,
            white
        )
    }

    fun renderTextureRect(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        u: Float,
        v: Float,
        regionWidth: Float,
        regionHeight: Float,
        textureWidth: Float,
        textureHeight: Float,
        leftTopColor: Int,
        leftBottomColor: Int,
        rightTopColor: Int,
        rightBottomColor: Int
    ) {
        renderTextureQuad(
            matrices,
            x,
            y,
            x + width,
            y + height,
            u,
            v,
            regionWidth,
            regionHeight,
            textureWidth,
            textureHeight,
            leftTopColor,
            leftBottomColor,
            rightTopColor,
            rightBottomColor
        )
    }

    fun renderTextureRect(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        u: Float,
        v: Float,
        regionWidth: Float,
        regionHeight: Float,
        textureWidth: Float,
        textureHeight: Float
    ) {
        renderTextureQuad(
            matrices,
            x,
            y,
            x + width,
            y + height,
            u,
            v,
            regionWidth,
            regionHeight,
            textureWidth,
            textureHeight
        )
    }

    fun setupRender() {
        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    fun endRender() {
        RenderSystem.disableBlend()
    }

    // -------------------- Utils --------------------

    fun setShaderColor(color: Int) {
        val a = (color shr 24 and 255).toFloat() / 255.0f
        val r = (color shr 16 and 255).toFloat() / 255.0f
        val g = (color shr 8 and 255).toFloat() / 255.0f
        val b = (color and 255).toFloat() / 255.0f
        RenderSystem.setShaderColor(r, g, b, a)
    }


    fun isHovered(mouseX: Double, mouseY: Double, x: Double, y: Double, width: Double, height: Double): Boolean {
        return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y
    }

    fun scrollAnimate(endPoint: Float, current: Float, speed0: Float): Float {
        var speed = speed0
        val shouldContinueAnimation = endPoint > current
        if (speed < 0.0f) {
            speed = 0.0f
        } else if (speed > 1.0f) {
            speed = 1.0f
        }
        val dif = max(endPoint, current) - min(endPoint, current)
        val factor = dif * speed
        return current + if (shouldContinueAnimation) factor else -factor
    }

    fun injectAlpha(color: Color, alpha: Int): Color {
        return Color(color.red, color.green, color.blue, alpha.coerceIn(0, 255))
    }

    fun TwoColoreffect(cl1: Color, cl2: Color, speed: Double): Color {
        val thing = speed / 4.0 % 1.0
        val `val` = MathHelper.clamp(sin(Math.PI * 6 * thing).toFloat() / 2.0f + 0.5f, 0.0f, 1.0f)
        return Color(
            lerp(cl1.red.toFloat() / 255.0f, cl2.red.toFloat() / 255.0f, `val`),
            lerp(cl1.green.toFloat() / 255.0f, cl2.green.toFloat() / 255.0f, `val`),
            lerp(cl1.blue.toFloat() / 255.0f, cl2.blue.toFloat() / 255.0f, `val`)
        )
    }

    fun lerp(a: Float, b: Float, f: Float): Float {
        return a + f * (b - a)
    }

    fun astolfo(yDist: Float, yTotal: Float, saturation: Float, speedt: Float): Color {
        var hue: Float
        val speed = 1800.0f
        hue = (System.currentTimeMillis() % speed.toInt().toLong()).toFloat() + (yTotal - yDist) * speedt
        while (hue > speed) {
            hue -= speed
        }
        if (speed.let { hue /= it; hue }.toDouble() > 0.5) {
            hue = 0.5f - (hue - 0.5f)
        }
        return Color.getHSBColor(0.5f.let { hue += it; hue }, saturation, 1.0f)
    }

    fun astolfo(clickgui: Boolean, yOffset: Int): Color {
        val speed = (if (clickgui) 35 * 100 else 30 * 100).toFloat()
        var hue = (System.currentTimeMillis() % speed.toInt() + yOffset).toFloat()
        if (hue > speed) {
            hue -= speed
        }
        hue /= speed
        if (hue > 0.5f) {
            hue = 0.5f - (hue - 0.5f)
        }
        hue += 0.5f
        return Color.getHSBColor(hue, 0.4f, 1f)
    }


    fun getColor2(hex: Int, alpha: Int): Color {
        val f1 = (hex shr 16 and 255).toFloat() / 255.0f
        val f2 = (hex shr 8 and 255).toFloat() / 255.0f
        val f3 = (hex and 255).toFloat() / 255.0f
        return Color((f1 * 255f).toInt(), (f2 * 255f).toInt(), (f3 * 255f).toInt(), alpha)
    }

    fun rainbow(delay: Int, saturation: Float, brightness: Float): Color {
        var rainbow = ceil(((System.currentTimeMillis() + delay) / 16f).toDouble())
        rainbow %= 360.0
        return Color.getHSBColor((rainbow / 360).toFloat(), saturation, brightness)
    }

    fun fadeColor(startColor: Int, endColor: Int, progress: Float): Int {
        var progress = progress
        if (progress > 1) {
            progress = 1 - progress % 1
        }
        return fade(startColor, endColor, progress)
    }

    fun fade(startColor: Int, endColor: Int, progress: Float): Int {
        val invert = 1.0f - progress
        val r = ((startColor shr 16 and 0xFF) * invert + (endColor shr 16 and 0xFF) * progress).toInt()
        val g = ((startColor shr 8 and 0xFF) * invert + (endColor shr 8 and 0xFF) * progress).toInt()
        val b = ((startColor and 0xFF) * invert + (endColor and 0xFF) * progress).toInt()
        val a = ((startColor shr 24 and 0xFF) * invert + (endColor shr 24 and 0xFF) * progress).toInt()
        return a and 0xFF shl 24 or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
    }

    fun skyRainbow(speed: Int, index: Int): Color {
        var angle = ((System.currentTimeMillis() / speed + index) % 360).toInt()
        val hue = angle / 360f
        return Color.getHSBColor(if ((360.0.also { angle %= it.toInt() } / 360.0).toFloat()
                .toDouble() < 0.5) -(angle / 360.0).toFloat() else (angle / 360.0).toFloat(), 0.5f, 1.0f)
    }

    fun fade(color: Color, delay: Int): Int {
        val hsb = FloatArray(3)
        Color.RGBtoHSB(color.red, color.green, color.blue, hsb)
        var brightness = abs((System.currentTimeMillis() % 2000L + delay).toFloat() / 1000.0f % 2f - 1.0f)
        brightness = 0.5f + 0.5f * brightness
        hsb[2] = brightness % 2.0f
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])
    }

    fun fade(speed: Int, index: Int, color: Color, alpha: Float): Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        var angle = ((System.currentTimeMillis() / speed + index) % 360).toInt()
        angle = (if (angle > 180) 360 - angle else angle) + 180
        val colorHSB = Color(Color.HSBtoRGB(hsb[0], hsb[1], angle / 360f))
        return Color(colorHSB.red, colorHSB.green, colorHSB.blue, max(0, min(255, (alpha * 255).toInt())))
    }

    fun getColor(color: Color, alpha: Int): Color {
        return Color(color.red, color.green, color.blue, alpha)
    }

    fun getAnalogousColor(color: Color): Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        val degree = 0.84f
        val newHueSubtracted = hsb[0] - degree
        return Color(Color.HSBtoRGB(newHueSubtracted, hsb[1], hsb[2]))
    }

    fun applyOpacity(color: Color, opacity: Float): Color {
        var opacity = opacity
        opacity = min(1f, max(0f, opacity))
        return Color(color.red, color.green, color.blue, (color.alpha * opacity).toInt())
    }

    fun applyOpacity(color_int: Int, opacity: Float): Int {
        var opacity = opacity
        opacity = min(1f, max(0f, opacity))
        val color = Color(color_int)
        return Color(color.red, color.green, color.blue, (color.alpha * opacity).toInt()).rgb
    }

    fun darker(color: Color, FACTOR: Float): Color {
        return Color(
            max((color.red * FACTOR).toInt(), 0),
            max((color.green * FACTOR).toInt(), 0),
            max((color.blue * FACTOR).toInt(), 0),
            color.alpha
        )
    }

    fun rainbow(speed: Int, index: Int, saturation: Float, brightness: Float, opacity: Float): Color {
        val angle = ((System.currentTimeMillis() / speed + index) % 360).toInt()
        val hue = angle / 360f
        val color = Color(Color.HSBtoRGB(hue, saturation, brightness))
        return Color(
            color.red, color.green, color.blue, 0.coerceAtLeast(255.coerceAtMost((opacity * 255).toInt()))
        )
    }

    fun interpolateColorsBackAndForth(speed: Int, index: Int, start: Color, end: Color, trueColor: Boolean): Color {
        var angle = ((System.currentTimeMillis() / speed + index) % 360).toInt()
        angle = (if (angle >= 180) 360 - angle else angle) * 2
        return if (trueColor) interpolateColorHue(start, end, angle / 360f) else interpolateColorC(
            start, end, angle / 360f
        )
    }

    fun interpolateColor(color1: Color, color2: Color, amount: Float): Int {
        var amount = amount
        amount = min(1f, max(0f, amount))
        return interpolateColorC(color1, color2, amount).rgb
    }

    fun interpolateColor(color1: Int, color2: Int, amount: Float): Int {
        var amount = amount
        amount = min(1f, max(0f, amount))
        val cColor1 = Color(color1)
        val cColor2 = Color(color2)
        return interpolateColorC(cColor1, cColor2, amount).rgb
    }

    fun interpolateColorC(color1: Color, color2: Color, amount: Float): Color {
        var amount = amount
        amount = min(1f, max(0f, amount))
        return Color(
            interpolateInt(color1.red, color2.red, amount.toDouble()),
            interpolateInt(color1.green, color2.green, amount.toDouble()),
            interpolateInt(color1.blue, color2.blue, amount.toDouble()),
            interpolateInt(color1.alpha, color2.alpha, amount.toDouble())
        )
    }

    fun interpolateColorHue(color1: Color, color2: Color, amount: Float): Color {
        var amount = amount
        amount = min(1f, max(0f, amount))
        val color1HSB = Color.RGBtoHSB(color1.red, color1.green, color1.blue, null)
        val color2HSB = Color.RGBtoHSB(color2.red, color2.green, color2.blue, null)
        val resultColor = Color.getHSBColor(
            interpolateFloat(color1HSB[0], color2HSB[0], amount.toDouble()),
            interpolateFloat(color1HSB[1], color2HSB[1], amount.toDouble()),
            interpolateFloat(color1HSB[2], color2HSB[2], amount.toDouble())
        )
        return Color(
            resultColor.red,
            resultColor.green,
            resultColor.blue,
            interpolateInt(color1.alpha, color2.alpha, amount.toDouble())
        )
    }

    fun interpolate(oldValue: Double, newValue: Double, interpolationValue: Double): Double {
        return oldValue + (newValue - oldValue) * interpolationValue
    }

    fun interpolateFloat(oldValue: Float, newValue: Float, interpolationValue: Double): Float {
        return interpolate(oldValue.toDouble(), newValue.toDouble(), interpolationValue.toFloat().toDouble()).toFloat()
    }

    fun interpolateInt(oldValue: Int, newValue: Int, interpolationValue: Double): Int {
        return interpolate(oldValue.toDouble(), newValue.toDouble(), interpolationValue.toFloat().toDouble()).toInt()
    }

    class Rectangle(var x: Float, var y: Float, var x1: Float, var y1: Float) {

        fun contains(x: Float, y: Float): Boolean {
            return x in this.x..this.x1 && y in this.y..this.y1
        }

        fun contains(x: Double, y: Double): Boolean {
            return contains(x.toFloat(), y.toFloat())
        }
    }

    fun drawRectGradient(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        leftBottomColor: Color,
        leftTopColor: Color,
        rightBottomColor: Color,
        rightTopColor: Color
    ) {
        val matrix = matrices.peek().positionMatrix
        val bufferBuilder = Tessellator.getInstance().buffer
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        bufferBuilder
            .vertex(matrix, x, y + height, 0.0f)
            .color(rightTopColor.red, rightTopColor.green, rightTopColor.blue, rightTopColor.alpha)
            .next()
        bufferBuilder
            .vertex(matrix, x + width, y + height, 0.0f)
            .color(leftTopColor.red, leftTopColor.green, leftTopColor.blue, leftTopColor.alpha)
            .next()
        bufferBuilder
            .vertex(matrix, x + width, y, 0.0f)
            .color(leftBottomColor.red, leftBottomColor.green, leftBottomColor.blue, leftBottomColor.alpha)
            .next()
        bufferBuilder
            .vertex(matrix, x, y, 0.0f)
            .color(rightBottomColor.red, rightBottomColor.green, rightBottomColor.blue, rightBottomColor.alpha)
            .next()
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
    }

    class BlurredShadow(bufferedImage: BufferedImage) {
        var id = Texture("texture/remote/" + RandomStringUtils.randomAlphanumeric(16))

        init {
            registerBufferedImageTexture(id, bufferedImage)
        }

        fun bind() {
            RenderSystem.setShaderTexture(0, id)
        }
    }
}
