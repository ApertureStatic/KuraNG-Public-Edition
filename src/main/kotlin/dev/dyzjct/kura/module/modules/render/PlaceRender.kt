package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.animations.Easing
import base.utils.graphics.ESPRenderer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import base.utils.math.scale
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

object PlaceRender : Module(
    name = "PlaceRender",
    langName = "放置渲染",
    description = "Draw Module's PlacePacket.",
    category = Category.RENDER
) {
    private val color = csetting("Color", Color(255, 255, 255))
    private val mode = msetting("Mode", Mode.Fade)
    private val fadeLength = isetting("FadeLength", 200, 0, 1000)
    private val fill = isetting("FillAlpha", 80, 0, 255)
    private val line = isetting("LineAlpha", 255, 0, 255)
    val renderBlocks = ConcurrentHashMap<BlockPos, Long>()

    init {
        onRender3D {
            runCatching {
                renderBlocks.forEach { (pos: BlockPos, time: Long) ->
                    if (System.currentTimeMillis() - time > fadeLength.value) {
                        renderBlocks.remove(pos)
                    } else {
                        val scale = Easing.IN_CUBIC.dec(Easing.toDelta(time, fadeLength.value))
                        val renderer = ESPRenderer()
                        var box = Box(pos)
                        when (mode.value) {
                            Mode.Fade -> box = box.scale(1.0)
                            Mode.Glide -> box = box.scale(scale.toDouble())
                        }
                        renderer.aFilled = (fill.value * scale).toInt()
                        renderer.aOutline = (line.value * scale).toInt()
                        renderer.add(
                            box, color.value
                        )
                        renderer.render(it.matrices, false)
                    }
                }

            }
        }
    }

    enum class Mode {
        Fade, Glide
    }
}