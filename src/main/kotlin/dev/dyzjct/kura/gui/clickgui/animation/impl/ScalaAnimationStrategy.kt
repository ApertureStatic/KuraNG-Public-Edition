package dev.dyzjct.kura.gui.clickgui.animation.impl

import dev.dyzjct.kura.gui.clickgui.animation.AbstractAnimationStrategy
import dev.dyzjct.kura.gui.clickgui.component.Component
import dev.dyzjct.kura.gui.clickgui.render.Alignment
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.system.util.interfaces.MinecraftWrapper
import net.minecraft.client.gui.DrawContext

class ScalaAnimationStrategy : AbstractAnimationStrategy() {
    override fun onBind(component: Component) {}

    override fun onUnbind(component: Component) {}

    override fun onRender(drawContext: DrawContext, mouseX: Float, mouseY: Float, component: Component) {
        drawContext.matrices.push()
        when (UiSetting.scalaDirection) {
            Alignment.START -> {
                drawContext.matrices.scale(progress, progress, 1f)
            }

            Alignment.CENTER -> {
                val scale = MinecraftWrapper.minecraft.window
                drawContext.matrices.translate(scale.scaledWidth / 2f, scale.scaledHeight / 2f, 0f)
                drawContext.matrices.scale(progress, progress, 1f)
                drawContext.matrices.translate(-scale.scaledWidth / 2f, -scale.scaledHeight / 2f, 0f)
            }

            Alignment.END -> {
                val scale = MinecraftWrapper.minecraft.window
                drawContext.matrices.translate(scale.scaledWidth.toDouble(), scale.scaledHeight.toDouble(), 0.0)
                drawContext.matrices.scale(progress, progress, 1f)
                drawContext.matrices.translate(-scale.scaledWidth.toDouble(), -scale.scaledHeight.toDouble(), 0.0)

            }
        }

        component.renderDelegate(drawContext, mouseX, mouseY)
        drawContext.matrices.pop()
    }
}