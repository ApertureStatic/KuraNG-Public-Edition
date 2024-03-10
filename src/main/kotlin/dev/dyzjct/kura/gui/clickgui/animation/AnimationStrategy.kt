package dev.dyzjct.kura.gui.clickgui.animation

import dev.dyzjct.kura.gui.clickgui.component.Component
import net.minecraft.client.gui.DrawContext

interface AnimationStrategy {
    fun onBind(component: Component)

    fun onUnbind(component: Component)

    fun onOpen()

    fun onClose()

    fun onRender(drawContext: DrawContext, mouseX: Float, mouseY: Float, component: Component)

    fun reset()

    fun playFinished(): Boolean
}