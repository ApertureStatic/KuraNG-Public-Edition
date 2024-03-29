package dev.dyzjct.kura.gui.clickgui.component

import dev.dyzjct.kura.gui.clickgui.render.Alignment
import dev.dyzjct.kura.gui.clickgui.render.DrawDelegate
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.gui.clickgui.render.Padding
import dev.dyzjct.kura.module.modules.client.UiSetting
import dev.dyzjct.kura.setting.BooleanSetting
import dev.dyzjct.kura.utils.animations.Easing
import java.awt.Color

class BooleanComponent(
    val booleanSetting: BooleanSetting,
    private val parentComponent: Component,
    override var x: Float,
    override var y: Float,
    override var width: Float,
    override var height: Float,
    override var drawDelegate: DrawDelegate
) : Component, Visible {
    private var startTime = System.currentTimeMillis()

    private val progress: Float
        get() = if (booleanSetting.value) {
            Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, 300f))
        } else {
            Easing.OUT_CUBIC.dec(Easing.toDelta(startTime, 300f))
        }

    override fun isVisible(): Boolean {
        return booleanSetting.isVisible()
    }

    override fun DrawScope.render(mouseX: Float, mouseY: Float) {
        drawRect(
            x,
            y,
            width * progress,
            height,
            if (isHovering(mouseX, mouseY)) {
                UiSetting.getThemeSetting().primary.darker()
            } else {
                UiSetting.getThemeSetting().primary
            }
        )

        drawText(
            booleanSetting.name, if (isHovering(mouseX, mouseY)) {
                Color.WHITE.darker()
            } else {
                Color.WHITE
            }, Padding(horizontal = 1f), verticalAlignment = Alignment.CENTER
        )
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        super.mouseClicked(mouseX, mouseY, button)
        if (isHovering(mouseX, mouseY) && button == 0) {
            booleanSetting.value = !booleanSetting.value
            parentComponent.rearrange()
            startTime = System.currentTimeMillis()
            return true
        }
        return false
    }
}