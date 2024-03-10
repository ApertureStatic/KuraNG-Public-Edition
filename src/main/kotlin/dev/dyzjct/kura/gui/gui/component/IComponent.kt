package dev.dyzjct.kura.gui.gui.component

import net.minecraft.client.gui.DrawContext

interface IComponent {
    var x: Double
    var y: Double
    var width: Double
    var height: Double

    fun render(context: DrawContext, mouseX: Double, mouseY: Double, partialTicks: Float)
    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean
    fun mouseReleased(mouseX: Double, mouseY: Double, state: Int)
    fun keyTyped(typedChar: Char, keyCode: Int)
    fun close()

    fun isHovered(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x.coerceAtMost(x + width) && mouseX <= x.coerceAtLeast(x + width) && mouseY >= y.coerceAtMost(y + height) && mouseY <= y.coerceAtLeast(
            y + height
        )
    }
}