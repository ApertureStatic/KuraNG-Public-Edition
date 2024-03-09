package dev.dyzjct.kura.gui.rewrite.gui.animation.impl

import dev.dyzjct.kura.gui.rewrite.gui.AlphaAnimationDrawDelegate
import dev.dyzjct.kura.gui.rewrite.gui.animation.AnimationStrategy
import dev.dyzjct.kura.gui.rewrite.gui.component.Component
import dev.dyzjct.kura.gui.rewrite.gui.render.DrawDelegate
import net.minecraft.client.gui.DrawContext

class AlphaAnimationStrategy(
    private val alphaAnimationDrawDelegate: AlphaAnimationDrawDelegate
) : AnimationStrategy {
    override fun onBind(component: Component) {
        component.changeDrawDelegate(alphaAnimationDrawDelegate)
    }

    override fun onUnbind(component: Component) {
        component.changeDrawDelegate(DrawDelegate.defaultDrawDelegate)
    }

    override fun onOpen() {
        alphaAnimationDrawDelegate.isReverse = false
        reset()
    }

    override fun onClose() {
        alphaAnimationDrawDelegate.isReverse = true
        reset()
    }

    override fun onRender(drawContext: DrawContext, mouseX: Float, mouseY: Float, component: Component) {
        component.renderDelegate(drawContext, mouseX, mouseY)
    }

    override fun reset() {
        alphaAnimationDrawDelegate.resetAnimation()
    }

    override fun playFinished(): Boolean {
        return alphaAnimationDrawDelegate.isAnimationFinished
    }
}