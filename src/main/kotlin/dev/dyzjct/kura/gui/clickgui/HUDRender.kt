package dev.dyzjct.kura.gui.clickgui

import dev.dyzjct.kura.gui.clickgui.component.SettingButton
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.module.modules.client.HUDEditor
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.util.function.Consumer

object HUDRender : Screen(Text.empty()) {
    var panels = ArrayList<Panel>()

    fun onCall() {
        var startX = 5.0
        for (category in Category.values()) {
            if (!category.isHUD) continue
            panels.add(Panel(category, startX, 5.0, 110.0, 15.0))
            startX += 120.0
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        for (i in panels.indices.reversed()) {
            panels[i].drawScreen(context, mouseX.toDouble(), mouseY.toDouble(), partialTicks)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
        for (panel in panels) {
            if (panel.mouseClicked(mouseX, mouseY, mouseButton)) {
                return false
            }
            if (!panel.extended) continue
            for (part in panel.elements) {
                if (part.mouseClicked(mouseX, mouseY, mouseButton)) {
                    return false
                }
                if (!part.isExtended) continue
                for (component in part.settings) {
                    if (component is SettingButton<*> && !component.value
                            .isVisible() || !component.mouseClicked(mouseX, mouseY, mouseButton)
                    ) continue
                    return false
                }
            }
        }
        return false
    }

    fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1) {
            ModuleManager.getModuleByClass(HUDEditor.javaClass).disable()
        }
        for (panel in panels) {
            panel.keyTyped(typedChar, keyCode)
        }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, state: Int): Boolean {
        for (panel in panels) {
            panel.mouseReleased(mouseX, mouseY, state)
        }
        return false
    }

    fun mouseDrag(dWheel: Double) {
        if (dWheel < 0) {
            panels.forEach(Consumer { component: Panel -> component.y -= 10 })
        } else if (dWheel > 0) {
            panels.forEach(Consumer { component: Panel -> component.y += 10 })
        }
    }

    fun getPanelByName(name: String): Panel? {
        var getPane: Panel? = null
        for (panel in panels) {
            if (panel.category.name != name) continue
            getPane = panel
        }
        return getPane
    }

    fun onClose() {
        panels.forEach {
            it.onClose()
        }
    }
}
