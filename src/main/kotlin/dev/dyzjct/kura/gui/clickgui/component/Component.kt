package dev.dyzjct.kura.gui.clickgui.component

import dev.dyzjct.kura.gui.clickgui.render.DrawDelegate
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.module.AbstractModule
import net.minecraft.client.gui.DrawContext

interface Component {
    var x: Float
    var y: Float

    var width: Float
    var height: Float

    var drawDelegate: DrawDelegate

    fun renderDelegate(context: DrawContext, mouseX: Float, mouseY: Float) {
        DrawScope(x, y, width, height, drawDelegate, context).render(mouseX, mouseY)
    }

    fun DrawScope.render(mouseX: Float, mouseY: Float) {}
    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        return false
    }

    fun mouseReleased(mouseX: Float, mouseY: Float, button: Int): Boolean {
        return false
    }

    fun keyTyped(keyCode: Int): Boolean {
        return false
    }

    fun rearrange() {}
    fun guiClosed() {}

    fun changeDrawDelegate(drawDelegate: DrawDelegate) {
        this.drawDelegate = drawDelegate
    }

    fun isHovering(mouseX: Float, mouseY: Float): Boolean {
        return mouseX in x..(x + width) && mouseY in y..(y + height)
    }
}