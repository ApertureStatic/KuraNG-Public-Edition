package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.animations.BlockEasingRender
import dev.dyzjct.kura.utils.animations.Easing
import melon.utils.graphics.ESPRenderer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import team.exception.melon.util.math.scale
import team.exception.melon.util.math.toBox
import java.awt.Color

object BlockHighlight : Module(name = "BlockHighlight", langName = "方块渲染", category = Category.RENDER) {
    private val movingLength by isetting("MovingLength", 400, 0, 1000)
    private val fadeLength by isetting("FadeLength", 200, 0, 1000)
    private val fillColor by csetting("FillColor", Color(255, 255, 255, 50))
    private val lineColor by csetting("LineColor", Color(255, 255, 255, 255))
    private val airPlace by bsetting("AirPlace", false)

    private var blockRenderSmooth = BlockEasingRender(movingLength.toFloat(), fadeLength.toFloat())
    private var startTime = 0L
    private var lastUpdatePos = BlockPos.ORIGIN
    private var first = false

    init {

        onRender3D { event ->
            val blockPos = (mc.crosshairTarget as? BlockHitResult)?.blockPos ?: return@onRender3D
            val isAir = world.isAir(blockPos)

            if (!isAir && !airPlace) {
                if (first) {
                    startTime = System.currentTimeMillis()
                    blockRenderSmooth.forceUpdatePos(blockPos)
                }
                first = false
            } else if (!first) {
                startTime = System.currentTimeMillis()
                first = true
            }

            lastUpdatePos = if (!isAir || airPlace) blockPos else lastUpdatePos

            val scale = if (airPlace) { 1.0f } else {
                if (isAir) {
                    Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength))
                } else {
                    Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength))
                }
            }

            blockRenderSmooth.updatePos(lastUpdatePos)

            val box = blockRenderSmooth.getUpdate().toBox().scale(scale.toDouble())

            val renderer = ESPRenderer()
            renderer.aFilled = (fillColor.alpha * scale).toInt()
            renderer.aOutline = (lineColor.alpha * scale).toInt()
            renderer.add(box, fillColor, lineColor)
            renderer.render(event.matrices, false)
        }
    }
}