package dev.dyzjct.kura.gui.clickgui.component

import base.system.render.graphic.Render2DEngine
import dev.dyzjct.kura.gui.clickgui.GuiScreen
import dev.dyzjct.kura.gui.clickgui.render.Alignment
import dev.dyzjct.kura.gui.clickgui.render.DrawDelegate
import dev.dyzjct.kura.gui.clickgui.render.DrawScope
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.modules.client.UiSetting
import java.awt.Color

class Panel(
    modules: List<dev.dyzjct.kura.module.AbstractModule>,
    val category: Category,
    private val guiScreen: GuiScreen,
    override var x: Float,
    override var y: Float,
    override var width: Float,
    override var height: Float,
    override var drawDelegate: DrawDelegate
) : ListComponent(guiScreen.container, height, elementSpacing = 0f) {
    private var dragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    private val beforeFilterModuleComponents = mutableListOf<ModuleComponent>()

    init {
        modules.forEach {
            beforeFilterModuleComponents.add(
                ModuleComponent(
                    it, this, x, y, width, height, drawDelegate
                )
            )
        }
        elements.addAll(beforeFilterModuleComponents)
        rearrange()
    }

    fun filterModules(condition: (String) -> Boolean) {
        elements.clear()
        elements.addAll(beforeFilterModuleComponents.filter { condition(it.module.moduleName) })
        playingAnimation = true
        rearrange()
    }

    override fun rearrange() {
        val y = y + 2f
        var offsetY = selfHeight
        elements.forEach {
            it.x = x
            it.y = y + offsetY
            it.rearrange()
            offsetY += it.height
        }
        totalHeight = offsetY + 4f

        if (!playingAnimation) {
            height = if (isOpened) totalHeight else height
            animationFlag.forceUpdate(totalHeight)
        }
    }

    override fun DrawScope.render(mouseX: Float, mouseY: Float) {
        if (dragging) {
            this@Panel.x = mouseX - dragOffsetX
            this@Panel.y = mouseY - dragOffsetY
            rearrange()
        }

        fillScopeBySetting(UiSetting.getThemeSetting().secondary)
        val primaryColor = UiSetting.getThemeSetting().primary

        if (UiSetting.getThemeSetting().panelBorder) {
            drawOutlineRectBySetting(
                x, y, width, height, primaryColor
            )
        }

        if (UiSetting.getThemeSetting().fillPanelTitle) {
            drawRectBySetting(x, y, width, selfHeight, primaryColor)
        }

        drawText(
            category.name,
            Color.WHITE,
            verticalAlignment = Alignment.CENTER,
            horizontalAlignment = Alignment.CENTER,
            containerHeight = selfHeight
        )

        renderChildElements(mouseX, mouseY)

        if (UiSetting.getThemeSetting().rect) {
            Render2DEngine.drawRectBlurredShadow(
                context.matrices,
                x - 8f,
                y - 8f,
                width + 16f,
                totalHeight + 16f,
                25,
                Color(primaryColor.red, primaryColor.green, primaryColor.blue, 30)
            )
        }

        if (isHovering(mouseX, mouseY)) {
            guiScreen.moveToFirstRender(this@Panel)
        }
    }

    override fun reduce() {
        elements.forEach {
            it.guiClosed()
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (isHovering(mouseX, mouseY)) {
            if (isHoveringOnTitle(mouseX, mouseY) && button == 0) {
                dragging = true
                dragOffsetX = mouseX - x
                dragOffsetY = mouseY - y
                return true
            }
            guiScreen.moveToFirstRender(this)
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (isHovering(mouseX, mouseY)) {
            if (dragging && button == 0) {
                dragging = false
                return true
            }
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }
}