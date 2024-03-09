package base.utils.graphics

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import base.system.render.graphic.Render2DEngine
import base.system.render.graphic.Render3DEngine
import base.system.render.graphic.mask.DirectionMask
import base.system.util.color.ColorRGB
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import java.awt.Color

class ESPRenderer {
    private var toRender0: MutableList<Info> = ArrayList()

    val toRender: List<Info>
        get() = toRender0

    var aFilled = 0
    var aOutline = 0
    var cOutLine = ColorRGB(Color.WHITE)
    var aTracer = 0
    var thickness = 2f
    var through = true
    var tracerOffset = 50

    val size: Int
        get() = toRender0.size

    fun add(pos: BlockPos, color: Color, outlineColor: Color = color) {
        add(pos, color, outlineColor, DirectionMask.ALL)
    }

    fun add(pos: BlockPos, color: ColorRGB, outlineColor: Color = Color(color.r, color.g, color.b, color.a)) {
        add(pos, color, outlineColor, DirectionMask.ALL)
    }

    fun add(pos: BlockPos, color: Color, outlineColor: Color = color, sides: Int) {
        add(Box(pos), color, outlineColor, sides)
    }

    fun add(
        pos: BlockPos, color: ColorRGB, outlineColor: Color = Color(color.r, color.g, color.b, color.a), sides: Int
    ) {
        add(Box(pos), color, outlineColor, sides)
    }

    fun add(box: Box, color: ColorRGB, outlineColor: Color = Color(color.r, color.g, color.b, color.a)) {
        add(box, color, outlineColor, DirectionMask.ALL)
    }

    fun add(box: Box, color: Color, outlineColor: Color = color) {
        add(box, ColorRGB(color), outlineColor, DirectionMask.ALL)
    }

    fun add(box: Box, color: Color, outlineColor: Color = color, sides: Int) {
        add(Info(box, ColorRGB(color), sides))
    }

    fun add(box: Box, color: ColorRGB, outlineColor: Color = Color(color.r, color.g, color.b, color.a), sides: Int) {
        add(Info(box, color, sides), outlineColor)
    }

    fun add(info: Info, outlineColor: Color = Color(info.color.r, info.color.g, info.color.b, info.color.a)) {
        cOutLine = ColorRGB(outlineColor)
        toRender0.add(info)
    }

    fun replaceAll(list: MutableList<Info>) {
        list.forEach {
            cOutLine = it.color
        }
        toRender0 = list
    }

    fun clear() {
        toRender0.clear()
    }

    fun render(stack: MatrixStack, clear: Boolean) {
        val filled = aFilled != 0
        val outline = aOutline != 0
        val tracer = aTracer != 0
        if (toRender0.isEmpty() || (!filled && !outline && !tracer)) return

        if (through) GlStateManager._disableDepthTest()
        RenderSystem.lineWidth(thickness)

        if (filled) {
            for ((box, color, _) in toRender0) {
                val a = (aFilled * (color.a / 255.0f)).toInt()
                //RenderUtils3D.drawBox(box, color.alpha(a), sides)
                Render3DEngine.drawFilledBox(
                    stack,
                    box,
                    Render2DEngine.injectAlpha(Color(color.r, color.g, color.b), a)
                )
            }
            //RenderUtils3D.draw(GL32.GL_TRIANGLES)
        }

        if (outline) {
            for ((box, color, _) in toRender0) {
                val a = (aOutline * ((if (cOutLine.a == color.a) color.a else cOutLine.a) / 255.0f)).toInt()
                Render3DEngine.drawBoxOutline(
                    box, Render2DEngine.injectAlpha(
                        if (cOutLine == ColorRGB(color.r, color.g, color.b)) Color(
                            color.r, color.g, color.b
                        ) else Color(
                            cOutLine.r, cOutLine.g, cOutLine.b
                        ), a
                    ), thickness
                )
            }
        }

        if (clear) clear()
        GlStateManager._enableDepthTest()
    }

    private enum class Type {
        FILLED, OUTLINE, TRACER
    }

    data class Info(val box: Box, val color: ColorRGB, val sides: Int) {
        constructor(box: Box) : this(box, ColorRGB(255, 255, 255), DirectionMask.ALL)
        constructor(box: Box, color: ColorRGB) : this(box, color, DirectionMask.ALL)
        constructor(pos: BlockPos) : this(Box(pos), ColorRGB(255, 255, 255), DirectionMask.ALL)
        constructor(pos: BlockPos, color: ColorRGB) : this(Box(pos), color, DirectionMask.ALL)
        constructor(pos: BlockPos, color: ColorRGB, sides: Int) : this(Box(pos), color, sides)
        constructor(pos: BlockPos, color: Color) : this(Box(pos), ColorRGB(color), DirectionMask.ALL)
        constructor(pos: BlockPos, color: Color, sides: Int) : this(Box(pos), ColorRGB(color), sides)
    }
}