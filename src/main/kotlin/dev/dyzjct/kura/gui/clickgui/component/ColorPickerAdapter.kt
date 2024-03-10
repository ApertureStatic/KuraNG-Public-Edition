package dev.dyzjct.kura.gui.clickgui.component

import dev.dyzjct.kura.gui.clickgui.render.DrawDelegate
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.setting.ColorSetting

class ColorPickerAdapter(
    private val colorSetting: ColorSetting,
    private val parentComponent: Component,
    override var x: Float,
    override var y: Float,
    override var width: Float,
    override var drawDelegate: DrawDelegate,
    initHeight: Float
) : Component, Visible {
    private val colorPicker = ColorPickerUtil(colorSetting, null, width.toDouble() + 12, initHeight.toDouble())

    override fun isVisible(): Boolean {
        return colorSetting.isVisible()
    }

    override var height: Float
        get() = colorPicker.height.toFloat()
        set(value) {}

    override fun rearrange() {
        colorPicker.x = x.toDouble() - 6
        colorPicker.y = y.toDouble() + 2
    }

    override fun DrawScope.render(mouseX: Float, mouseY: Float) {
        parentComponent.rearrange()

        colorPicker.render(matrixStack, mouseX.toDouble(), mouseY.toDouble())
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        colorPicker.mouseClicked(mouseX.toDouble(), mouseY.toDouble(), button)
        return false
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int): Boolean {
        colorPicker.mouseReleased(mouseX.toDouble(), mouseY.toDouble(), button)
        return false
    }
}