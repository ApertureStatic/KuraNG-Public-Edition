package dev.dyzjct.kura.gui.clickgui.component

import dev.dyzjct.kura.module.modules.client.UiSetting
import net.minecraft.client.gui.DrawContext
import java.awt.Color

class PinnedComponent(
    private val component: Component,
    var fillBackground: Boolean = false,
    var backgroundColor: Color = UiSetting.getThemeSetting().secondary
) : Component by component {
    override var x: Float
        get() = component.x
        set(value) {}

    override var y: Float
        get() = component.y
        set(value) {}

    override var width: Float
        get() = component.width
        set(value) {}

    override var height: Float
        get() = component.height
        set(value) {}

    override fun renderDelegate(context: DrawContext, mouseX: Float, mouseY: Float) {
        if (fillBackground) {
            drawDelegate.drawReact(
                context.matrices, x, y, width, height, backgroundColor
            )
        }
        super.renderDelegate(context, mouseX, mouseY)
    }

    override fun rearrange() {
    }
}