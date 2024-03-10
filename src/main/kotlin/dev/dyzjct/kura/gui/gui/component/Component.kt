package dev.dyzjct.kura.gui.gui.component

import dev.dyzjct.kura.gui.gui.Panel
import net.minecraft.client.MinecraftClient

abstract class Component : IComponent {
    var mc: MinecraftClient = MinecraftClient.getInstance()
    override var x = 0.0
    override var y = 0.0
    override var width = 0.0
    override var height = 0.0
    var father: Panel? = null
    var isToggled = false
    var isExtended = false
    override fun mouseReleased(mouseX: Double, mouseY: Double, state: Int) {}
    override fun keyTyped(typedChar: Char, keyCode: Int) {}
    override fun close() {}

    fun solvePos() {
        father?.let {
            x = it.x
            y = it.y
        }
    }

    override fun isHovered(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x.coerceAtMost(x + width) && mouseX <= x.coerceAtLeast(x + width) && mouseY >= y.coerceAtMost(y + height) && mouseY <= y.coerceAtLeast(
            y + height
        )
    }
}
