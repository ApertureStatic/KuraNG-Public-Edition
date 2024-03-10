package dev.dyzjct.kura.module.modules.render

import dev.dyzjct.kura.manager.BlockFinderManager
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.system.render.graphic.Render3DEngine
import base.utils.math.toBox
import java.awt.Color

object PortalESP : Module(
    name = "PortalESP",
    langName = "地狱门透视",
    category = Category.RENDER,
    description = "AutoSearch For Portal Blocks!"
) {
    val distance by isetting("Range", 50, 1, 80)
    private var lineWidth by fsetting("LineWidth", 1.5f, 0f, 5f)
    private var colorFill by csetting("ColorFill", Color(49, 231, 229, 100))
    private var colorLine by csetting("ColorLine", Color(49, 231, 229, 255))

    init {
        onRender3D { event ->
            BlockFinderManager.portalBlockList.forEach {
                Render3DEngine.drawFilledBox(event.matrices, it.toBox(), colorFill)
                Render3DEngine.drawBoxOutline(it.toBox(), colorLine, lineWidth)
            }
        }
    }
}