package dev.dyzjct.kura.gui.rewrite.gui.component

import dev.dyzjct.kura.gui.rewrite.gui.render.DrawDelegate
import dev.dyzjct.kura.module.modules.client.UiSetting
import net.minecraft.client.gui.DrawContext
import java.awt.Color

class ComponentContainer(
    private var fillBackground: Boolean = false,
    var color: Color = UiSetting.getThemeSetting().secondary,
    private val elements: MutableList<Component> = mutableListOf(),
    override var drawDelegate: DrawDelegate = DrawDelegate.defaultDrawDelegate,
) : Component, MutableList<Component> by elements {
    override var x: Float = 0f
    override var y: Float = 0f
    override var width: Float = 0f
    override var height: Float = 0f

    override fun changeDrawDelegate(drawDelegate: DrawDelegate) {
        super.changeDrawDelegate(drawDelegate)
        elements.forEach { it.changeDrawDelegate(drawDelegate) }
    }

    override fun renderDelegate(context: DrawContext, mouseX: Float, mouseY: Float) {
        if (fillBackground) {
            val x = elements.maxOf { it.x }
            val y = elements.minOf { it.y }
            val width = elements.maxOf { it.width }
            val height = elements.maxOf { it.height }

            drawDelegate.drawReact(context.matrices, x, y, width, height, color)
        }
        elements.forEach { it.renderDelegate(context, mouseX, mouseY) }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        return elements.any { it.mouseClicked(mouseX, mouseY, button) }
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int): Boolean {
        return elements.any { it.mouseReleased(mouseX, mouseY, button) }
    }

    override fun keyTyped(keyCode: Int): Boolean {
        return elements.any { it.keyTyped(keyCode) }
    }

    override fun guiClosed() {
        elements.forEach { it.guiClosed() }
    }
}