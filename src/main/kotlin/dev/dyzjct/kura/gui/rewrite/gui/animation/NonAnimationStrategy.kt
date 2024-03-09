package dev.dyzjct.kura.gui.rewrite.gui.animation

import dev.dyzjct.kura.gui.rewrite.gui.component.Component
import dev.dyzjct.kura.gui.rewrite.gui.render.DrawDelegate
import net.minecraft.client.gui.DrawContext

object NonAnimationStrategy : AnimationStrategy {
    override fun onBind(component: Component) {
        component.changeDrawDelegate(DrawDelegate.defaultDrawDelegate)
    }

    override fun onUnbind(component: Component) {
    }

    override fun onOpen() {
    }

    override fun onClose() {
    }

    override fun onRender(drawContext: DrawContext, mouseX: Float, mouseY: Float, component: Component) {
        component.renderDelegate(drawContext, mouseX, mouseY)
    }

    override fun reset() {
    }

    override fun playFinished(): Boolean {
        return true
    }
}